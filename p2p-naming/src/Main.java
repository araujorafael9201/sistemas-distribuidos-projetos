public class Main {
    public static void main(String[] args) {
        Node p0 = new Node(8886, 8881, 8882, "p0", true);
        Node p1 = new Node(8881, 8882, 8883, "p1", false);
        Node p2 = new Node(8882, 8883, 8884, "p2", false);
        Node p3 = new Node(8883, 8884, 8885, "p3", false);
        Node p4 = new Node(8884, 8885, 8886, "p4", false);
        Node p5 = new Node(8885, 8886, 8881, "p5", false);
        
        p1.run();
        p2.run();
        p3.run();
        p4.run();
        p5.run();

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        p0.run();
    }
}