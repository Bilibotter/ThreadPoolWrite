package test;

import util.Hint;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestEligibleTermination {
    public static void main(String[] args) throws InterruptedException {
        TestThreadPoolExecutor executor = new TestThreadPoolExecutor(4, 6, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        executor.execute(new Hint());
        executor.execute(new Hint());
        executor.execute(new Hint());
        executor.execute(new Hint());
        executor.execute(new Hint());
        executor.execute(new Hint());
        Thread.sleep(500);
        executor.shutdown();
    }
}
