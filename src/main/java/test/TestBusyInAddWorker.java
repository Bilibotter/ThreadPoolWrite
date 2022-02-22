package test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestBusyInAddWorker {
    public static void main(String[] args) {
        Runnable command = () -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignore) {}
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 8, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2));
    }
}
