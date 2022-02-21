import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LinkedBlockingQueueTest {
    public static void main(String[] args) {
        Runnable command = () -> {
            LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>(16);
            try {
                queue.poll(1000, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }
}
