package src;

// import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Cook extends Thread {
  public int cookID;
  public int cookTime;
  public int numTable;
  ArrayBlockingQueue<Diner> orderPool;
  ArrayBlockingQueue<Diner> tablePool;
  Machine lockMachineBurger;
  Machine lockMachineFries;
  Machine lockMachineCoke;
  GlobalClock globalClock;

  public Cook(int cookID, GlobalClock globalClock, ArrayBlockingQueue<Diner> orderPool,
      ArrayBlockingQueue<Diner> tablePool,
      Machine lockMachineBurger, Machine lockMachineFries,
      Machine lockMachineCoke, int numTable) {
    this.cookID = cookID;
    this.orderPool = orderPool;
    this.tablePool = tablePool;
    this.lockMachineBurger = lockMachineBurger;
    this.lockMachineFries = lockMachineFries;
    this.lockMachineCoke = lockMachineCoke;
    this.globalClock = globalClock;
    this.numTable = numTable;
  }

  @Override
  public String toString() {
    return String.format("This is cook %d.\n", this.cookID);
  }

  @Override
  public void run() {
    try {
      while (globalClock.dinerCount != 0) {
        // Diner diner = orderPool.take();
        Diner diner = orderPool.poll();
        if (diner != null) {
          // cook start processing this order
          this.cookTime = Math.max(this.cookTime, diner.seatedTime);
          diner.orderProcessTime = this.cookTime;
          System.out.format("[%d] Diner %d's order will be processed by Cook %d.\n", this.cookTime, diner.dinerID,
              this.cookID);

          // keep try to process the foods until down
          while (diner.orderBurger != 0 || diner.orderFries != 0 || diner.orderCoke != 0) {
            // TODO: freeTime is set as volatile, should it garantee correctness?
            int minMachineFreeTime = 300;
            if (diner.orderBurger != 0) {
              // cook can try to use machine
              synchronized (lockMachineBurger) {
                if (lockMachineBurger.freeTime <= this.cookTime) {
                  // cook get this machine
                  lockMachineBurger.freeTime = this.cookTime + 5;
                  diner.orderBurger--;
                  System.out.format("[%d] Cook %d takes the machine for Buckeye Burger.\n", this.cookTime, this.cookID);
                  this.cookTime = lockMachineBurger.freeTime;
                } else {
                  // machine is not available
                  minMachineFreeTime = Math.min(lockMachineBurger.freeTime, minMachineFreeTime);
                }
              }
            }

            if (diner.orderFries != 0) {
              synchronized (lockMachineFries) {
                if (lockMachineFries.freeTime <= this.cookTime) {
                  lockMachineFries.freeTime = this.cookTime + 3;
                  diner.orderFries--;
                  System.out.format("[%d] Cook %d takes the machine for Brutus Fries.\n", this.cookTime, this.cookID);
                  this.cookTime = lockMachineFries.freeTime;
                } else {
                  minMachineFreeTime = Math.min(lockMachineFries.freeTime, minMachineFreeTime);
                }
              }
            }

            if (diner.orderCoke != 0) {
              synchronized (lockMachineCoke) {
                if (lockMachineCoke.freeTime <= this.cookTime) {
                  lockMachineCoke.freeTime = this.cookTime + 1;
                  diner.orderCoke--;
                  System.out.format("[%d] Cook %d takes the machine for Coke.\n", this.cookTime, this.cookID);
                  this.cookTime = lockMachineCoke.freeTime;
                } else {
                  minMachineFreeTime = Math.min(lockMachineCoke.freeTime, minMachineFreeTime);
                }
              }
            }

            // if cook can not use all the machines, set cook free time to the minimum free
            // machine time
            if (minMachineFreeTime == 300) {
              // cook just not get the lock
            } else {
              this.cookTime = minMachineFreeTime;
            }
          }

          // process down, order is ready
          System.out.format("[%d] Diner %d's food is ready. Diner %d starts to eat.\n", this.cookTime, diner.dinerID,
              diner.dinerID);
          diner.orderFinishTime = this.cookTime;
          diner.leaveTime = this.cookTime + 30;
          TimeUnit.SECONDS.sleep(1);

          // set global clock to the minimum leaveTime around all tables
          synchronized (tablePool) {
            Object[] element = tablePool.toArray();
            // if table has free slot, and has diner waiting, not move clock
            if (element.length == this.numTable) {
              // table if full, set to the minimum of all tables
              int oldGlobalClock = globalClock.timestamp;
              globalClock.timestamp = 300;
              for (int i = 0; i < element.length; i++) {
                int curLeaveTime = ((Diner) element[i]).leaveTime;
                if (curLeaveTime == 200) {
                  // diner order is not ready, don't consider it
                  globalClock.timestamp = oldGlobalClock;
                  break;
                } else {
                  // continue finding the minimum leave time around all tables
                  globalClock.timestamp = Math.min(globalClock.timestamp, curLeaveTime);
                }
              }
            } else if (globalClock.dinerWaiting == 0) {
              // table is not full, but if no diner waiting, also need to move clock
              // at least one table's order is ready, just set clock to that order
              globalClock.timestamp = 300; // reset to a large number, try to find minimum
              for (int i = 0; i < element.length; i++) {
                int curLeaveTime = ((Diner) element[i]).leaveTime;
                if (curLeaveTime == 200) {
                  // diner order is not ready, don't consider it
                } else {
                  // continue finding the minimum leave time around all tables
                  globalClock.timestamp = Math.min(globalClock.timestamp, curLeaveTime);
                }
              }
            }
          }

          // this diner can be take from table pool,
          // free the table with global clock >= this.leaveTime (any one satisfies this)
          synchronized (tablePool) {
            Object[] element = tablePool.toArray();
            // System.out.println("tablePool try to take one diner " + element.length);
            ArrayBlockingQueue<Diner> tempPool = new ArrayBlockingQueue<>(element.length);
            for (int i = 0; i < element.length; i++) {
              Diner dinerTemp = tablePool.take();
              if (dinerTemp.leaveTime <= globalClock.timestamp) {
                // target find, put diner in temp pool back
                Object[] tempElement = tempPool.toArray();
                for (int j = 0; j < tempElement.length; j++) {
                  Diner backDiner = tempPool.take();
                  tablePool.put(backDiner);
                }
                System.out.format("[%d] Diner %d leaves.\n", dinerTemp.leaveTime, dinerTemp.dinerID);
                globalClock.dinerCount--;
                // System.out.println("counter: " + globalClock.dinerCount);
                break;
              } else {
                tempPool.put(dinerTemp);
              }
            }
          }
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
}
