import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class RMQChannelPool {
  private final BlockingQueue<Channel> pool;
  private int capacity;
  private RMQChannelFactory factory;

  public RMQChannelPool(int maxSize, RMQChannelFactory factory) {
    this.capacity = maxSize;
    this.pool = new LinkedBlockingQueue<>(capacity);
    this.factory = factory;
    for(int i = 0; i < capacity; i++) {
      Channel chan;
      try {
        chan=factory.create();
        pool.add(chan);
      } catch (IOException e) {
        System.out.println("Error creating channel" + e.getMessage());
      }
    }
  }

  public Channel get() {
    try {
      return pool.take();
    } catch (InterruptedException e) {
      throw new RuntimeException("Error: no channels available" + e.toString());
    }
  }


  public void returnChannel(Channel chan) {
    if(chan != null) {
      pool.add(chan);
    }
  }

}
