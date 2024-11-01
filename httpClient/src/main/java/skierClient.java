import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class skierClient {
  private static int initialThread = 32;
  private static int totalRequest = 200000;
  private static String url = "http://35.91.173.128:8080/SkierServer_war";
  private static int requestPerThread = 1000;


   public static void main(String[] args) throws InterruptedException {
     BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(5000);

     long startTime = System.currentTimeMillis();
     HttpClient httpClient = HttpClient.newHttpClient();
     Thread eventThread = new Thread(new LiftRideEventGenerator(eventQueue, totalRequest));
     eventThread.start();

     AtomicInteger failRequest = new AtomicInteger(0);
     AtomicInteger successRequest = new AtomicInteger(0);
     ExecutorService executorService = Executors.newCachedThreadPool();

     for(int i = 0; i < initialThread; i++) {
       executorService.submit(new LiftRideEventPoster(eventQueue, requestPerThread, url, failRequest, httpClient, successRequest));
     }

     int remainingRequests = totalRequest - (initialThread*requestPerThread);
     while(remainingRequests > 0) {
       requestPerThread = Math.min(remainingRequests, 1000);
       executorService.submit(new LiftRideEventPoster(eventQueue, requestPerThread, url, failRequest, httpClient, successRequest));
       remainingRequests -= requestPerThread;
     }
     executorService.shutdown();

//     LiftRideEventPoster test = new LiftRideEventPoster(eventQueue, 1000, url, failRequest, httpClient, successRequest);
//     Thread testThread = new Thread(test);
//     testThread.start();

     try {
       boolean finished = executorService.awaitTermination(1, TimeUnit.HOURS);
       if (!finished) {
         System.out.println("Timeout reached before all tasks completed.");
       }
     } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
       System.out.println("Execution interrupted.");
     }

//     try {
//       testThread.join();
//     } catch (InterruptedException e) {
//       Thread.currentThread().interrupt();
//       System.out.println("Event generator interrupted.");
//     }

     try {
       eventThread.join();
     } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
       System.out.println("Event generator interrupted.");
     }
     long endTime = System.currentTimeMillis();
     long totalTime = endTime - startTime;

     double totalTimeInSeconds = totalTime / 1000.0;
     double throughput = totalRequest / totalTimeInSeconds;


     System.out.println("Time taken: " + totalTime);
     System.out.println("Failed request: " + failRequest.get());
     System.out.println("Success request: " + successRequest.get());
     System.out.println("Throughput: " + throughput);
     System.out.println("Initial Threads: " + initialThread);
     System.out.println("Total Requests: " + totalRequest);
     System.out.println("Requests per Thread: " + requestPerThread);
   }
}
