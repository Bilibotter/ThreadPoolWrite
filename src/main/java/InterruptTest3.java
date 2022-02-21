public class InterruptTest3 {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(() -> {
            for (long i = 0; i < Long.MAX_VALUE; i++) {
                if (i % 10000 == 0) {
                    System.out.println(i);
                    long temp = 0;
                    for (long j = 0; j < Integer.MAX_VALUE; j++) {
                        temp = j;
                    }
                    System.out.println(temp);
                }
            }
        });
        thread.start();
        System.out.println("start");
        thread.interrupt();
        System.out.println("interrupt");
        Thread.sleep(2000);
    }
}
