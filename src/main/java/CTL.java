public class CTL {
    private static final int CAP = (1 << (Integer.SIZE-3)) - 1;
    private static int ctlOf(int rs, int wc) { return rs | wc; }
    private static int runStateOf(int c)     { return c & ~CAP; }
    private static int workerCountOf(int c)  { return c & CAP; }
    public static void main(String[] args) {
        // -536870912
        final int RUNNING = -1 << (Integer.SIZE-3);
        // RUNNING
        final int INIT_CTL = ctlOf(RUNNING, 0);
        // 0
        final int SHUTDOWN = 0 << (Integer.SIZE - 3);
        final int STOP       =  1 << (Integer.SIZE - 3);
        final int TIDYING    =  2 << (Integer.SIZE - 3);
        final int TERMINATED =  3 << (Integer.SIZE - 3);
        // 00011111111111111111111111111111
        final int CAPACITY   = (1 << (Integer.SIZE-3)) - 1;
        // 11100000000000000000000000000000
        final int REVERT = ~ CAPACITY;
        System.out.println("CAPACITY:  000"+Integer.toBinaryString(CAPACITY)+", size:"+Integer.toBinaryString(CAPACITY).length());
        System.out.println("REVERT:    "+Integer.toBinaryString(REVERT)+", size:"+Integer.toBinaryString(REVERT).length());
        System.out.println("RUNNING:   "+Integer.toBinaryString(RUNNING)+", size:"+Integer.toBinaryString(RUNNING).length());
        System.out.println("RUNNING+1: "+Integer.toBinaryString(RUNNING+1)+", size:"+Integer.toBinaryString(RUNNING+1).length());
        System.out.println("RUNNING+3: "+Integer.toBinaryString(RUNNING+3)+", size:"+Integer.toBinaryString(RUNNING+3).length());
        System.out.println("RUNNING+7: "+Integer.toBinaryString(RUNNING+7)+", size:"+Integer.toBinaryString(RUNNING+7).length());
        System.out.println("SHUTDOWN:  0000000000000000000000000000000"+Integer.toBinaryString(SHUTDOWN)+", size:"+Integer.toBinaryString(SHUTDOWN).length());
        System.out.println("STOP:      00"+Integer.toBinaryString(STOP)+", size:"+Integer.toBinaryString(STOP).length());
        System.out.println("TIDYING:   0"+Integer.toBinaryString(TIDYING)+", size:"+Integer.toBinaryString(TIDYING).length());
        System.out.println("TERMINATED:0"+Integer.toBinaryString(TERMINATED)+", size:"+Integer.toBinaryString(TERMINATED).length());
        return;
    }
}
