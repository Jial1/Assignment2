
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.DeliverCallback;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.util.logging.Logger;


@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {
  private static final Gson gson = new Gson();
  private static final int MIN_LIFT_ID = 1;
  private static final int MAX_LIFT_ID = 40;
  private static final int MIN_SKIER_ID = 1;
  private static final int MAX_SKIER_ID = 1_000_000;
  private static final int MIN_RESORT_ID = 1;
  private static final int MAX_RESORT_ID = 10;
  private static final int MIN_TIMESTAMP = 1;
  private static final int MAX_TIMESTAMP = 360;
  private static final int VALID_SEASON_ID = 2024;
  private static final int VALID_DAY_ID = 1;
  private static final String TASK_QUEUE_NAME = "liftRideEvent_Queue";
  private static final int NUM_THREADS = 200;
  private static final int NUM_CHAN = 20;
  private Connection rabbitMQConnection;

  @Override
  public void init() throws ServletException {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("35.93.197.189");
      factory.setPort(5672);
      factory.setUsername("Jiali1");
      factory.setPassword("12345");
      rabbitMQConnection = factory.newConnection();
    } catch (IOException | TimeoutException e) {
      throw new ServletException("Failed to establish RabbitMQ connection", e);
    }
  }

//  @Override
//  public void destroy() {
//    if (rabbitMQConnection != null) {
//      try {
//        rabbitMQConnection.close();
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//    }
//  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");

    String uri = req.getRequestURI();

    String[] urlParts = uri.split("/");
    if (!isUrlValid(urlParts)) {
      sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, "Invalid URL format");
      return;
    }

    LiftRideEvent liftRideEvent;
    try {
      int resortID = Integer.parseInt(urlParts[urlParts.length - 7]);
      int seasonID = Integer.parseInt(urlParts[urlParts.length - 5]);
      int dayID = Integer.parseInt(urlParts[urlParts.length - 3]);
      int skierID = Integer.parseInt(urlParts[urlParts.length - 1]);

      String requestData = readRequestBody(req);
      System.out.println(requestData);

      if (requestData.isEmpty()) {
        sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Request body is empty");
        return;
      }

      liftRideEvent = gson.fromJson(requestData, LiftRideEvent.class);

      if (liftRideEvent == null) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      if (!isInputValid(resortID, seasonID, dayID, skierID, liftRideEvent.getTime(), liftRideEvent.getLiftID(), res) && !isInputValid(
          liftRideEvent.getResortID(), Integer.parseInt(liftRideEvent.getSeasonID()), Integer.parseInt(liftRideEvent.getDayID()),
          liftRideEvent.getSkierID(), liftRideEvent.getTime(), liftRideEvent.getLiftID(), res)) {
        return;
      }
      SuccessResponse successResponse = new SuccessResponse("POST request processed", liftRideEvent);
      String jsonResponse = gson.toJson(successResponse);
      processRequest(jsonResponse, rabbitMQConnection);

      PrintWriter out = res.getWriter();
      out.write(jsonResponse);

      res.setStatus(HttpServletResponse.SC_CREATED);

    } catch (JsonSyntaxException e) {
      sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
    }
  }

  public void processRequest(String jsonResponse, Connection connection) {

    try {
      RMQChannelFactory channelFactory = new RMQChannelFactory(connection);
      RMQChannelPool pool = new RMQChannelPool(NUM_CHAN, channelFactory);
      ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

      for (int i = 0; i < NUM_THREADS; i++) {
        executor.submit(() -> {
          try {
            Channel channel = pool.get();
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            channel.basicPublish("", TASK_QUEUE_NAME, null, jsonResponse.getBytes());
            pool.returnChannel(channel);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
      }

      executor.shutdown();
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }





  private String readRequestBody(HttpServletRequest req) throws IOException {
    StringBuilder requestData = new StringBuilder();
    try (BufferedReader reader = req.getReader()) {
      String line;
      while ((line = reader.readLine()) != null) {
        requestData.append(line);
      }
    }
    return requestData.toString().trim();
  }


  private void sendErrorResponse(HttpServletResponse res, int statusCode, String message)
      throws IOException {
    res.setStatus(statusCode);
    ErrorResponse errorResponse = new ErrorResponse(message);
    String jsonError = gson.toJson(errorResponse);
    PrintWriter out = res.getWriter();
    out.write(jsonError);
  }


  private boolean isInputValid(int resortID, int seasonID, int dayID, int skierID, int time, int liftID, HttpServletResponse res)
      throws IOException {
    if (liftID < MIN_LIFT_ID || liftID > MAX_LIFT_ID) {
      sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,
          "Lift ID should be between " + MIN_LIFT_ID + " and " + MAX_LIFT_ID);
      return false;
    }
    if (skierID < MIN_SKIER_ID || skierID > MAX_SKIER_ID) {
      sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,
          "Skier ID should be between " + MIN_SKIER_ID + " and " + MAX_SKIER_ID);
      return false;
    }
    if (resortID < MIN_RESORT_ID || resortID > MAX_RESORT_ID) {
      sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,
          "Resort ID should be between " + MIN_RESORT_ID + " and " + MAX_RESORT_ID);
      return false;
    }
    if (time < MIN_TIMESTAMP || time > MAX_TIMESTAMP) {
      sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,
          "Timestamp should be between " + MIN_TIMESTAMP + " and " + MAX_TIMESTAMP);
      return false;
    }
    if (seasonID != VALID_SEASON_ID || dayID != VALID_DAY_ID) {
      sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,
          "Season ID must be " + VALID_SEASON_ID + " and Day ID must be " + VALID_DAY_ID);
      return false;
    }
    return true;
  }


  private boolean isUrlValid(String[] urlPath) {
    if (urlPath == null || urlPath.length != 10) {
      return false;
    }
    if(!urlPath[2].equals("skiers") || !urlPath[4].equals("seasons") || !urlPath[6].equals("days") || !urlPath[8].equals("skiers")) {
      return false;
    }
    try {
      Integer.parseInt(urlPath[3]);
      Integer.parseInt(urlPath[5]);
      Integer.parseInt(urlPath[7]);
      Integer.parseInt(urlPath[9]);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }
}