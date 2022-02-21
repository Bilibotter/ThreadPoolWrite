package test;

import java.util.concurrent.ThreadPoolExecutor;

public class OnlySleep implements Runnable {

    private ThreadPoolExecutor executor;

    public OnlySleep(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void run() {
        executor.execute(() -> {
            try {
                Thread.sleep(10000);
                System.out.println("Finish sleep");
            } catch (InterruptedException ignore) {}
        });
    }
}
