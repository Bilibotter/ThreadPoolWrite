package util;

public class Hint implements Runnable {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " is running!");
    }
}
