package test;

import java.util.concurrent.LinkedBlockingQueue;

public class TestInterrupt {
    public static void main(String[] args) throws InterruptedException {
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        queue.take();
        System.out.println("Finish");
    }
}
