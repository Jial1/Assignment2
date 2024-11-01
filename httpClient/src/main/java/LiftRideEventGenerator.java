import java.util.concurrent.BlockingQueue;

public class LiftRideEventGenerator implements Runnable {
  private final int totalRequest;
  private BlockingQueue<LiftRideEvent> eventQueue;

  public LiftRideEventGenerator(BlockingQueue<LiftRideEvent> eventQueue, int totalRequest) {
    this.eventQueue = eventQueue;
    this.totalRequest = totalRequest;
  }


  @Override
  public void run() {
    for (int i = 0; i < totalRequest; i++) {
      LiftRideEvent event = new LiftRideEvent();
      try{
        eventQueue.put(event);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
}
