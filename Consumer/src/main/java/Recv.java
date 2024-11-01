
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Recv {
  private final static String QUEUE_NAME = "liftRideEvent_Queue";
  private final static int NUM_Thread = 200;

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("44.243.83.103");
    factory.setPort(5672);
    factory.setUsername("Jiali1");
    factory.setPassword("12345");
    Connection connection = factory.newConnection();

    ConcurrentHashMap<Integer, List<LiftRideEvent>> liftRide = new ConcurrentHashMap<>();
    ExecutorService executorService = Executors.newFixedThreadPool(NUM_Thread);

    for(int i=0; i<NUM_Thread; i++) {
      executorService.execute(new ProcessLiftRide(liftRide, QUEUE_NAME, connection));
    }

  }
}
