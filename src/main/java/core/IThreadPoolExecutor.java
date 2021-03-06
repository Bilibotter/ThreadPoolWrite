package core;

import org.apache.maven.surefire.shade.org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.maven.surefire.shade.org.apache.commons.lang3.builder.HashCodeBuilder;
import core.IBlockingQueue;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
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

    private final Condition termination = mainLock.newCondition();

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

    final void reject(Runnable command) {
        throw new RejectedExecutionException("Reject command!");
    }

    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {

        final Thread thread;

        Runnable firstTask;

        public Worker(Runnable firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        @Override
        public void run() {
            runWorker(this);
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
                               IBlockingQueue<Runnable> workQueue) {
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null)
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
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);
            /*
            if (rs != prev && (rs >= SHUTDOWN &&!
                    // 运作状态为SHUTDOWN只在以下情况可以添加成功
                    // 任务为null且阻塞队列还有未执行完的任务
                    (rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty()) )) {
                return false;
            }
             */
            if (rs != prev && (rs > SHUTDOWN ||
                    // 允许shutdown状态下添加在shutdown前提交的任务
                    (rs == SHUTDOWN && firstTask == null && workQueue.isEmpty()) )) {
                return false;
            }
            int wc = workerCountOf(c);
            // corePoolSize和maximumPoolSize可能改变，要能感知到这种变化
            int accept = core ? corePoolSize : maximumPoolSize;
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
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            if (rs >= STOP || (rs == SHUTDOWN && workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            timeout = allowCoreThreadTimeOut && timeout;

            if (wc > maximumPoolSize ||
                    (wc >= corePoolSize && (workQueue.isBusy() || timeout) )) {
                if (compareAndDecrementWorkerCount(c)) {
                    return null;
                }
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
                    task = null;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }

    }

    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // 正常退出的线程getTask==null且执行了workCount-1
        if (completedAbruptly) {
            decrementWorkerCount();
        }
        mainLock.lock();
        try {
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        tryTerminate();

        int c = ctl.get();
        // 1. 处理核心超时控制模式及扩容模式的线程销毁
        // 2. 处理shutdown及running的异常退出
        if (c < STOP) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                // 也许核心线程获取任务超时但workQueue仍有任务
                if (min == 0 && !workQueue.isEmpty()) {
                    min = 1;
                }
                if (workerCountOf(c) >= min) {
                    return;
                }
            }
            // 如果是扩容模式异常退出core=true则会无法恢复到扩容模式
            // addWorker并不会使worker数量超过maximum
            addWorker(null, false);
        }
    }

    public void shutdown() {
        mainLock.lock();
        try {
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers(false);
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            if (runStateOf(c) >= targetState || ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) {
                return;
            }
        }
    }

    // 懒得返回任务列表
    public void shutdownNow() {
        List<Runnable> tasks;
        mainLock.lock();
        try {
            advanceRunState(STOP);
            interruptWorkers();
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            if (isRunning(c) || c >= TIDYING || (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) {
                return;
            }
            if (workerCountOf(c) != 0) {
                interruptIdleWorkers(true);
                return;
            }
            mainLock.lock();
            try {
                // 暂时不扩展terminate，直接terminated
                ctl.compareAndSet(c, TERMINATED);
                // 唤醒await的信号量
                termination.signalAll();
            } finally {
                mainLock.unlock();
            }
        }
    }

    private void interruptIdleWorkers(boolean onlyOne) {
        mainLock.lock();
        try {
            for (Worker worker : workers) {
                Thread t = worker.thread;
                if (!t.isInterrupted() && worker.tryLock()) {
                    try {
                        t.interrupt();
                    } finally {
                        worker.unlock();
                    }
                }
                // 关于onlyOne
                // 1.interrupt:使在getTask这一步骤阻塞的僵尸线程被唤醒，线程被唤醒后会转而尝试唤醒其他的一个阻塞线程并结束本线程
                // 2.do nothing:只要work里面仍有运行的线程，则可以由运行线程来代替本线程去中断被阻塞的线程
                if (onlyOne) {
                    break;
                }
            }
        } finally {
            mainLock.unlock();
        }
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
