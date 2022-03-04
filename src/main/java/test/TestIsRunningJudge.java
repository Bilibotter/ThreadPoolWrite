package test;

import util.MyQueue;
import util.MyRunnable;
import util.OnlySleep;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestIsRunningJudge {
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 8, 8, TimeUnit.SECONDS, new MyQueue<Runnable>(16));
        Thread t = new Thread(new MyRunnable(executor));
        Thread s1 = new Thread(new OnlySleep(executor));
        s1.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        t.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        // 即使shutdown前已经提交了命令也会拒绝命令
        executor.shutdown();
    }
}
