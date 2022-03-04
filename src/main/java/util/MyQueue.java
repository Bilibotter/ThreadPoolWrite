package util;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

public class MyQueue<E> extends LinkedBlockingQueue<E> {

    public MyQueue() {
    }

    public MyQueue(int capacity) {
        super(capacity);
    }

    public MyQueue(Collection<? extends E> c) {
        super(c);
    }

    @Override
    public boolean offer(E e) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignore) {}
        return super.offer(e);
    }
}
