import java.security.AccessController;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class IThreadPoolExecutor implements Executor {
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int RUNNING = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;
    private static final int UNEXIST =  4 << COUNT_BITS;
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
    private final AtomicInteger ctl = new AtomicInteger(RUNNING);

    private volatile int corePoolSize;

    private volatile int maximumPoolSize;

    private volatile long keepAliveTime;

    private final BlockingQueue<Runnable> workQueue;

    private volatile ThreadFactory threadFactory;

    private final ReentrantLock mainLock = new ReentrantLock();

    /** 与高3位并 */
    private static int runStateOf(int ctl)                  {return ctl & ~CAPACITY;}
    /** 与低28位并 */
    private static int workerCountOf(int ctl)               {return ctl & CAPACITY;}
    /** 高位运行状态与worker数量构成ctl */
    private static int ctlOf(int runState, int workerCount) {return runState | workerCount;}

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    final void reject(Runnable command) {}

    public IThreadPoolExecutor(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               BlockingQueue<Runnable> workQueue,
                               ThreadFactory threadFactory,
                               RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        // 使用默认线程池工厂来命名线程
        this.threadFactory = Executors.defaultThreadFactory();
    }

    @Override
    public void execute(Runnable command) {
        // addWorker(null, xxx)的操作是从阻塞队列获取任务
        // 因此不允许command为空
        if (command == null) {
            throw new NullPointerException();
        }
        int c = ctl.get();
        if (!isRunning(c)) {
            reject(command);
        }
        // 区别于juc对running的判断
        // 如果添加任务时仍是running，则会执行该任务而不是拒绝(shutdownNow不会执行)
        boolean addSuc = false;
        // 获取锁来阻止shutdown
        mainLock.lock();
        try {
            // recheck在以下情景会拒绝任务
            // || isRunning || execute(command) || wQ.offer(command) || shutdown || removeWorker(commandAddInRunning)
            if (workerCountOf(c) < corePoolSize && addWorker(command, true)) {
                return;
            }
            addSuc = workQueue.offer(command);
        } finally {
            mainLock.unlock();
        }
        if (!addSuc && !addWorker(command, false)) {
            reject(command);
        }
    }

    private boolean addWorker(Runnable firstTask, boolean core) {
        int prev = UNEXIST;
        int accept = core ? corePoolSize : maximumPoolSize;
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);
            // 原本的判断也太啰嗦了
            if (rs != prev && rs >= STOP || (rs == SHUTDOWN && workQueue.isEmpty())) {
                return false;
            }
            int wc = workerCountOf(c);
            if (wc >= CAPACITY || wc >= accept) {
                return false;
            }
            if (ctl.compareAndSet(c, c+1)) {
                break;
            }
            prev = rs;
        }
    }



    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }
}
