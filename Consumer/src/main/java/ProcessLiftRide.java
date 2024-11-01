import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

public class ProcessLiftRide implements Runnable {
  private ConcurrentHashMap<Integer, List<LiftRideEvent>> liftRide;
  private String json;
  private final String QUEUE_NAME;
  private final ConnectionFactory factory;

  public ProcessLiftRide(ConcurrentHashMap<Integer, List<LiftRideEvent>> liftRide, String QUEUE_NAME,
      ConnectionFactory factory) {
    this.liftRide = liftRide;
    this.QUEUE_NAME = QUEUE_NAME;
    this.factory = factory;
  }

  @Override
  public void run() {
    try(Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

      channel.queueDeclare(QUEUE_NAME, true, false, false, null);

      channel.basicQos(1);
      System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        LiftRideEvent liftRideEvent = parseLiftRideEvent(message);


        updateSkierLiftRides(liftRideEvent);
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        System.out.println( "Callback thread ID = " + Thread.currentThread().getId() + " Received '" + message + "'");
      };
      // process messages
      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException(e);
    }

  }

  private void updateSkierLiftRides(LiftRideEvent liftRideEvent) {
    int skierID = liftRideEvent.getSkierID();
    liftRide.computeIfAbsent(skierID, k -> Collections.synchronizedList(new ArrayList<>())).add(liftRideEvent);
  }

  private LiftRideEvent parseLiftRideEvent(String message) {
    return new Gson().fromJson(message, LiftRideEvent.class);
  }


}
