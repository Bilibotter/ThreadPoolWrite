package test;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TouchFishDelayQueue<E> extends LinkedBlockingQueue<E> implements IBlockingQueue<E> {
    private double threshold = 0.3;

    public TouchFishDelayQueue(double threshold) {
        this.threshold = threshold;
    }

    public TouchFishDelayQueue(int capacity, double threshold) {
        super(capacity);
        this.threshold = threshold;
    }

    public TouchFishDelayQueue(Collection<? extends E> c, double threshold) {
        super(c);
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public E take() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        return super.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        return super.poll(timeout, unit);
    }

    @Override
    public E poll() {
        return super.poll();
    }

    @Override
    public boolean isBusy() {
        double s = this.size();
        return (s / (remainingCapacity()+s)) > threshold;
    }
}
