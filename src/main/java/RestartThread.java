public class RestartThread {
    public static void main(String[] args) {
        Thread thread = new Thread(()-> {
            System.out.println("ThreadStart");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        thread.start();
    }
}
