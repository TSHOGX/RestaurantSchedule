package src;

public class Diner {
  public int arriveTime;
  public int orderBurger;
  public int orderFries;
  public int orderCoke;
  public int dinerID;
  public volatile int tableID;
  public int cookID;
  public int seatedTime;
  public int orderProcessTime;
  public int orderFinishTime;
  public int leaveTime = 200; // means order is not set; 300 means seated but don't know when to leave

  public Diner(int arriveTime, int orderBurger, int orderFries, int orderCoke, int dinerID) {
    this.arriveTime = arriveTime;
    this.orderBurger = orderBurger;
    this.orderFries = orderFries;
    this.orderCoke = orderCoke;
    this.dinerID = dinerID;
    this.leaveTime = 300;
  }

  @Override
  public String toString() {
    return String.format("Diner %d", this.dinerID);
  }
}
