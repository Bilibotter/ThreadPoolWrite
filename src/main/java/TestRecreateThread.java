import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestRecreateThread {
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor threadExecutor = new MyExecutor(19, 24,  5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(16));
        Runnable command = () -> {
            System.out.println("新线程");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("结束");
        };
        threadExecutor.execute(command);
        Thread.sleep(4000);
        System.out.println();
        Thread.sleep(4000);
        System.out.println();
        threadExecutor.shutdown();
        return;
    }
}
