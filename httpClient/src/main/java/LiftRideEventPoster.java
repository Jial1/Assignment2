import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class LiftRideEventPoster implements Runnable {
  private final BlockingQueue<LiftRideEvent> eventQueue;
  private final int requestPerThread;
  private final String serverURL;
  private final AtomicInteger failRequest;
  private final HttpClient httpClient;
  private final AtomicInteger successRequest;

  public LiftRideEventPoster(BlockingQueue<LiftRideEvent> eventQueue, int requestPerThread, String url,
      AtomicInteger failRequest, HttpClient httpClient, AtomicInteger successRequest) {
    this.eventQueue = eventQueue;
    this.requestPerThread = requestPerThread;
    this.serverURL = url;
    this.failRequest = failRequest;
    this.httpClient = httpClient;
    this.successRequest = successRequest;
  }

  @Override
  public void run() {
    for (int i = 0; i < requestPerThread; i++) {
      try{
        LiftRideEvent liftRideEvent = eventQueue.take();
        String dynamicUrl = String.format(
            "%s/skiers/%d/seasons/2024/days/1/skiers/%d",
            serverURL,
            liftRideEvent.getResortID(),
            liftRideEvent.getSkierID()
        );
        boolean success = sendPostRequest(liftRideEvent.toJson(), 0, URI.create(dynamicUrl));
        if(!success) {
          failRequest.incrementAndGet();
        } else {
          successRequest.incrementAndGet();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private boolean sendPostRequest(String json, int retry, URI dynamicUrl) {
    if (retry >= 5) {
      return false;
    }

    HttpRequest httpRequest = HttpRequest.newBuilder().uri(dynamicUrl)
        .header("content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json)).build();

    try {
      HttpResponse<String> res = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      int responseCode = res.statusCode();
      if (responseCode == 201) {
        System.out.println("Data sent: " + res.body());
        return true;
      } else {
        System.out.println("Response Code: " + res.body());
        return sendPostRequest(json, retry + 1, dynamicUrl);
      }
    } catch (Exception e) {
      if(e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return sendPostRequest(json, retry + 1, dynamicUrl);
    }
  }
}
