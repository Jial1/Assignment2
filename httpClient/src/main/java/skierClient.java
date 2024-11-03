import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class skierClient {
  private static int initialThread = 32;
  private static int totalRequest = 200000;
  private static String url = "http://35.81.158.173:8080/SkierServer_war";
  private static int requestPerThread = 1000;
  private static List<performance> performanceList = new CopyOnWriteArrayList<>();
  private static File csvFile = new File("latency_records.csv");


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
       executorService.submit(new LiftRideEventPoster(eventQueue, requestPerThread, url, failRequest, httpClient, successRequest, performanceList));
     }

     int remainingRequests = totalRequest - (initialThread*requestPerThread);
     while(remainingRequests > 0) {
       requestPerThread = Math.min(remainingRequests, 1000);
       executorService.submit(new LiftRideEventPoster(eventQueue, requestPerThread, url, failRequest, httpClient, successRequest, performanceList));
       remainingRequests -= requestPerThread;
     }
     executorService.shutdown();


     try {
       boolean finished = executorService.awaitTermination(1, TimeUnit.HOURS);
       if (!finished) {
         System.out.println("Timeout reached before all tasks completed.");
       }
     } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
       System.out.println("Execution interrupted.");
     }


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
     writeToCSV();


     System.out.println("Time taken: " + totalTime);
     System.out.println("Failed request: " + failRequest.get());
     System.out.println("Success request: " + successRequest.get());
     System.out.println("Throughput: " + throughput);
     System.out.println("Initial Threads: " + initialThread);
     System.out.println("Total Requests: " + totalRequest);
     System.out.println("Requests per Thread: " + requestPerThread);
     calculateLatencyStats();
   }

  private static void writeToCSV() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    try (FileWriter csvWriter = new FileWriter(csvFile)) {
      csvWriter.append("start_time, request_type, latency, response_code\n");
      for (performance task : performanceList) {
        String formattedTime = sdf.format(new Date(task.getStartTime()));
        csvWriter.append(String.format("%s,POST,%d,%d\n", formattedTime, task.getLatency(),
            task.getResponse_code()));
      }
    } catch (IOException e) {
      System.out.println("Error writing CSV file: " + e.getMessage());
    }
  }

  public static void calculateLatencyStats() {
    if (performanceList.isEmpty()) {
      System.out.println("No performance data available.");
      return;
    }

    // Extract latencies from the performance list
    List<Long> latencies = performanceList.stream()
        .map(performance::getLatency)
        .sorted()
        .collect(Collectors.toList());


    long minLatency = Collections.min(latencies);
    long maxLatency = Collections.max(latencies);
    double meanLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
    long medianLatency = latencies.get(latencies.size() / 2);
    long p99Latency = latencies.get((int) Math.ceil(0.99 * latencies.size()) - 1);


    System.out.println("Latency Statistics:");
    System.out.println("Min latency: " + minLatency + " ms");
    System.out.println("Max latency: " + maxLatency + " ms");
    System.out.println("Mean latency: " + meanLatency + " ms");
    System.out.println("Median latency: " + medianLatency + " ms");
    System.out.println("99th percentile latency: " + p99Latency + " ms");
  }
}
