public class GlobalClock {
  public volatile int timestamp = 0;
  public volatile int dinerCount;
  public volatile int dinerWaiting;

  public GlobalClock(int timestamp, int dinerCount, int dinerWaiting) {
    this.timestamp = timestamp;
    this.dinerCount = dinerCount;
    this.dinerWaiting = dinerWaiting;
  }
}
