package test;

import java.util.concurrent.ThreadPoolExecutor;

public class MyRunnable implements Runnable {
    private ThreadPoolExecutor executor;

    public MyRunnable(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void run() {
        executor.execute(() -> {
            System.out.println(Thread.currentThread().getName() + " running!");
        });
    }
}
