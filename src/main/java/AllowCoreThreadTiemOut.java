import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AllowCoreThreadTiemOut {
    public static void main(String[] args) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(8, 16, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(20));
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }
}
