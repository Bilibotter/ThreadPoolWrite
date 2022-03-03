package test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestShutdown {
    static volatile int disableResort = 0;
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 6, 2, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        Runnable r = () -> {
            System.out.println(Thread.currentThread().getName() + " is running!");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                System.out.println("Interrupt");
            }
        };
        executor.execute(r);
        executor.execute(r);
        Thread.sleep(100);
        System.out.println("Pool size " + executor.getPoolSize());
        executor.shutdown();
        disableResort = 1;
    }
}
