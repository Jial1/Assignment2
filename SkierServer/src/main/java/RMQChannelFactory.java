import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class RMQChannelFactory extends BasePooledObjectFactory<Channel> {
  ConnectionFactory factory = new ConnectionFactory();

  @Override
  public Channel create() throws Exception {
    factory.setHost("52.35.158.255");
    factory.setPort(5672);
    factory.setUsername("jiali");
    factory.setPassword("12345");
    Connection connection = factory.newConnection();
    return connection.createChannel();
  }

  @Override
  public PooledObject<Channel> wrap(Channel channel) {
    return new DefaultPooledObject<>(channel);
  }

  @Override
  public void destroyObject(PooledObject<Channel> p) throws Exception {
    p.getObject().close();
  }

}
