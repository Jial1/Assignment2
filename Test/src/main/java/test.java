import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class test {
  public static void main(String[] args) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("44.243.83.103");
    factory.setPort(5672);
    factory.setUsername("Jiali1");
    factory.setPassword("12345");

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {

      String queueName = "testQueue";
      channel.queueDeclare(queueName, false, false, false, null);
      String message = "Test message!";
      channel.basicPublish("", queueName, null, message.getBytes());
      System.out.println(" [x] Sent '" + message + "'");
    }
  }
}
