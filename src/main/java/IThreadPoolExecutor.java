import org.apache.maven.surefire.shade.org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.maven.surefire.shade.org.apache.commons.lang3.builder.HashCodeBuilder;
import test.IBlockingQueue;

import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

public class IThreadPoolExecutor implements Executor {
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;
    private static final int NOT_EXIST  =  4 << COUNT_BITS;
    private static final int CAPACITY   =  (1 << COUNT_BITS) - 1;
    private final AtomicInteger ctl     = new AtomicInteger(RUNNING);

    private volatile int corePoolSize;

    private volatile int maximumPoolSize;

    private volatile long keepAliveTime;

    private volatile boolean allowCoreThreadTimeOut = false;

    private final IBlockingQueue<Runnable> workQueue;

    private volatile ThreadFactory threadFactory;

    private final ReentrantLock mainLock = new ReentrantLock();

    private final HashSet<Worker> workers = new HashSet<Worker>();

    /** 与高3位并 */
    private static int runStateOf(int ctl)                  {return ctl & ~CAPACITY;}
    /** 与低28位并 */
    private static int workerCountOf(int ctl)               {return ctl & CAPACITY;}
    /** 高位运行状态与worker数量构成ctl */
    private static int ctlOf(int runState, int workerCount) {return runState | workerCount;}

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    final void reject(Runnable command) {}

    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {

        final Thread thread;

        volatile long completedTask;

        Runnable firstTask;

        public Worker(Runnable firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        @Override
        public void run() {

        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        @Override
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            Worker worker = (Worker) o;

            return new EqualsBuilder().append(thread, worker.thread).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(thread).toHashCode();
        }

        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    public IThreadPoolExecutor(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               IBlockingQueue<Runnable> workQueue,
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
        int prev = NOT_EXIST;
        int accept = core ? corePoolSize : maximumPoolSize;
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);
            if (rs != prev && (rs >= SHUTDOWN &&!
                    // 运作状态为SHUTDOWN只在以下情况可以添加成功
                    // 任务为null且阻塞队列还有未执行完的任务
                    (rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty()) )) {
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
        Worker w;
        final Thread t;
        mainLock.lock();
        try {
            w = new Worker(firstTask);
            t = w.thread;
            if (t == null) {
                throw new NullPointerException("ThreadPool factory provided thread is null.");
            }
            // 区别于JUC
            // 任务是否可添加取决execute提交那一刻的状态，因此不做recheck
            if (t.isAlive()) {
                throw new IllegalStateException("Attempt to restart a thread which has started!");
            }
            workers.add(w);
        } finally {
            mainLock.unlock();
        }
        boolean workerStarted = false;
        try {
            t.start();
            workerStarted = true;
        } finally {
            if (!workerStarted) {
                addWorkerFailed(w);
            }
        }
        return true;
    }

    private void addWorkerFailed(Worker worker) {
        mainLock.lock();
        try {
            workers.remove(worker);
            decrementWorkerCount();
        } finally {
            mainLock.unlock();
        }
    }

    private Runnable getTask() {
        boolean timeout = false;
        boolean timed;
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            if (rs >= STOP || (rs == SHUTDOWN && workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            timed = allowCoreThreadTimeOut || wc >= corePoolSize;

            if (wc > maximumPoolSize || (timed && timeout && workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }

            try {
                Runnable task = workerCountOf(c) >= corePoolSize ?
                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (task != null) {
                    return task;
                }
                timeout = true;
            } catch (InterruptedException e) {
                timeout = false;
            }
        }
    }

    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        // 将Worker的state变量由-1变成0以启动
        w.unlock();
        boolean completedAbruptly = true;

        try {
            while (task != null || (task = getTask()) != null) {
                // 不允许shutdown中断线程池
                w.lock();
                try {
                    task.run();
                } finally {
                    w.unlock();
                    w.completedTask++;
                }
            }
            completedAbruptly = false;
        } finally {

        }

    }

    private void processWorkerExit(Worker w, boolean completedAbruptly) {

    }

    private void decrementWorkerCount() {
        for (;;) {
            int curr = ctl.get();
            if (ctl.compareAndSet(curr, curr-1)) {
                break;
            }
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

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public boolean isAllowCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }
}
