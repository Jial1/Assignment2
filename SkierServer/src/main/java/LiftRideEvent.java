import java.util.Objects;

public class LiftRideEvent {
  private int skierID;
  private int resortID;
  private int liftID;
  private String seasonID;
  private String dayID;
  private int time;

  public LiftRideEvent(int skierID, int resortID, int liftID, String seasonID, String dayID, int time) {
    this.skierID = skierID;
    this.resortID = resortID;
    this.liftID = liftID;
    this.seasonID = seasonID;
    this.dayID = dayID;
    this.time = time;
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

  public String getSeasonID() {
    return seasonID;
  }

  public String getDayID() {
    return dayID;
  }

  public int getTime() {
    return time;
  }

  public void setResortID(int resortID) {
    this.resortID = resortID;
  }

  public void setSkierID(int skierID) {
    this.skierID = skierID;
  }

  public void setLiftID(int liftID) {
    this.liftID = liftID;
  }

  public void setSeasonID(String seasonID) {
    this.seasonID = seasonID;
  }

  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  public void setTime(int time) {
    this.time = time;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LiftRideEvent that = (LiftRideEvent) o;
    return skierID == that.skierID && resortID == that.resortID && liftID == that.liftID
        && seasonID.equals(that.seasonID) && dayID.equals(that.dayID) && time == that.time;
  }


  @Override
  public int hashCode() {
    return Objects.hash(skierID, resortID, liftID, seasonID, dayID, time);
  }

  @Override
  public String toString() {
    return "LiftRideEvent{" +
        "skierID=" + skierID +
        ", resortID=" + resortID +
        ", liftID=" + liftID +
        ", seasonID=" + seasonID +
        ", dayID=" + dayID +
        ", time=" + time +
        '}';
  }
}
