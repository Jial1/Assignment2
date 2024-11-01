import java.util.concurrent.ThreadLocalRandom;

public class LiftRideEvent {
  private int skierID;
  private int resortID;
  private int liftID;
  private String seasonId = "2024";
  private String dayID = "1";
  private int time;

  public LiftRideEvent() {
    this.skierID = ThreadLocalRandom.current().nextInt(1, 100001);
    this.resortID = ThreadLocalRandom.current().nextInt(1, 11);
    this.liftID = ThreadLocalRandom.current().nextInt(1, 41);
    this.time = ThreadLocalRandom.current().nextInt(1, 361);
  }

  public int getSkierID() {
    return skierID;
  }

  public int getResortID() {
    return resortID;
  }

  public int getLiftID() {
    return liftID;
  }

  public int getTime() {
    return time;
  }

  public String toJson() {
    return "{"
        + "\"skierID\":" + skierID + ","
        + "\"resortID\":" + resortID + ","
        + "\"liftID\":" + liftID + ","
        + "\"seasonId\":" + seasonId + ","
        + "\"dayID\":" + dayID + ","
        + "\"time\":" + time
        + "}";
  }
}

