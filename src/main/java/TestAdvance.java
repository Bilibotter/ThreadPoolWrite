import test.Hint;
import test.TouchFishDelayQueue;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestAdvance {
    public static void main(String[] args) throws InterruptedException {
        IThreadPoolExecutor executor1 = new IThreadPoolExecutor(4, 16,  3, TimeUnit.SECONDS, new TouchFishDelayQueue<>(10));
        for (int _ = 0; _ < 10; _++) {
            executor1.execute(new Hint());
        }
        executor1.shutdown();
        Thread.sleep(15000);
        System.out.println("Finish executor1");
    }
}
