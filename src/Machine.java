package src;

public class Machine {
  public volatile int freeTime;

  public Machine(int freeTime) {
    this.freeTime = freeTime;
  }
}
