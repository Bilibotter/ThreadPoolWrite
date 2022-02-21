public class InteruptTest {
    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(()-> {
            System.out.println("Interrupted? "+Thread.currentThread().isInterrupted());
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                System.out.println("Interrupt");
                System.out.println("Interrupted? "+Thread.currentThread().isInterrupted());
            }
            try {
                Thread.sleep(4000);
                System.out.println("Run normally");
                System.out.println("Interrupted? "+Thread.currentThread().isInterrupted());
            } catch (InterruptedException e) {
                System.out.println("Interrupt * 2;");
            }

        });
        t.start();
        Thread.sleep(1000);
        t.interrupt();
        System.out.println(t.isAlive());
    }
}
