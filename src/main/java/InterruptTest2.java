public class InterruptTest2 {
    public static void main(String[] args) {
        Thread thread = new Thread(()-> {
            int i = 0;
            while (true) {
               System.out.println(i++);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        thread.interrupt();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(thread.isAlive());
        System.out.println(thread.getState());Z
    }
}
