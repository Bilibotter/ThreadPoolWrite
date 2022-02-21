import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestShutdownNow {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(4,8, 2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(16));
        Runnable command = () -> {
            for (int i = 0; i < Integer.MAX_VALUE; i++ ) {
                if (i % 1000000 == 0) {
                    System.out.println(i);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        System.out.println("收到中断请求");
                        return;
                    }
                }
            }
        };
        executor.execute(command);
        executor.shutdownNow();
        System.out.println("shutdown");
    }
}
