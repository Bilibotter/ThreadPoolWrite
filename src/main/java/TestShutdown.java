import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestShutdown {
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(4,8, 2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(16));
        Runnable command1 = () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("command-1接到中断请求*1");
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println("command-1接到中断请求*2,并退出");
            }
            System.out.println("command-1不受中断影响");
        };
        Runnable command2 = () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("command-2接到中断请求*1并退出");
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println("command-2接到中断请求*2,并退出");
                return;
            }
            System.out.println("command-2不受中断影响");
        };
        executor.execute(command1);
        executor.execute(command2);
        Thread.sleep(1000);
        executor.shutdown();
        System.out.println("shutdown");
    }
}
