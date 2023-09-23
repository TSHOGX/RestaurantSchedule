
// import java.io.File;
// import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Restaurant {
  public static Machine lockMachineBurger = new Machine(0);
  public static Machine lockMachineFries = new Machine(0);
  public static Machine lockMachineCoke = new Machine(0);
  public static GlobalClock globalClock;
  public static Object lock = new Object();
  public static ArrayBlockingQueue<Diner> tablePool;
  public static ArrayBlockingQueue<Diner> orderPool;

  public static int getTableID(Diner diner, ArrayBlockingQueue<Diner> tablePool) {
    Object[] element = tablePool.toArray();
    for (int i = 0; i < element.length; i++) {
      if (((Diner) element[i]).dinerID == diner.dinerID) {
        return i;
      }
    }
    return -1;
  }

  public static void main(String[] args) {
    Scanner scanner;
    try {
      File file = new File(args[0]);
      scanner = new Scanner(file);
    } catch (FileNotFoundException | ArrayIndexOutOfBoundsException e) {
      System.out.println("Usage: java Restaurant 'filePath'");
      return;
    }

    int numDiner = Integer.parseInt(scanner.nextLine());
    int numTable = Integer.parseInt(scanner.nextLine());
    int numCook = Integer.parseInt(scanner.nextLine());

    System.out.format("In Restaurant 6431, there are %d tables, %d cooks, and %d dinner will be coming.\n", numTable,
        numCook, numDiner);

    //
    globalClock = new GlobalClock(0, numDiner, numDiner);

    // init two global pool
    tablePool = new ArrayBlockingQueue<>(numTable);
    orderPool = new ArrayBlockingQueue<>(numTable);

    // start all cook threads
    ExecutorService executor = Executors.newFixedThreadPool(numCook);
    try {
      for (int i = 0; i < numCook; i++) {
        executor.execute(
            new Cook(i, globalClock, orderPool, tablePool, lockMachineBurger, lockMachineFries, lockMachineCoke,
                numTable));
      }
    } catch (Exception err) {
      err.printStackTrace();
    }

    // restaurant open, diner came in...
    int dinerIDCount = 0;
    while (scanner.hasNextLine()) {
      String[] nums = scanner.nextLine().split(",");
      int arriveTime = Integer.parseInt(nums[0]);

      // create new diner
      Diner diner = new Diner(arriveTime, Integer.parseInt(nums[1]), Integer.parseInt(nums[2]),
          Integer.parseInt(nums[3]), dinerIDCount);
      System.out.format("[%d] Diner %d arrives.\n", diner.arriveTime, diner.dinerID);
      dinerIDCount++;
      globalClock.dinerWaiting--;

      // try to get the table, if no free table, just block and wait
      try {
        tablePool.put(diner);
        diner.tableID = getTableID(diner, tablePool);
        diner.seatedTime = Math.max(globalClock.timestamp, diner.arriveTime); // TODO check timestamp
        System.out.format("[%d] Diner %d is seated at table %d.\n", diner.seatedTime, diner.dinerID, diner.tableID);
        orderPool.put(diner);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    scanner.close();

    while (true) {
      if (globalClock.dinerCount == 0) {
        executor.shutdown();
        System.out.format("[%d] The last diner has left and the restaurant is to be closed.\n", globalClock.timestamp);
        break;
      }
    }

    System.out.println("END");
  }
}
