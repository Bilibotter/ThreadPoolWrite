package core;


import java.util.concurrent.BlockingQueue;

public interface IBlockingQueue<E> extends BlockingQueue<E> {
    /** 当前队列是否需要使用扩容模式来处理任务 */
    boolean isBusy();
}
