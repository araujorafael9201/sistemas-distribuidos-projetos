import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Integer id = null;
        while (id == null) {
            System.out.print("Node id (0-5): ");
            if (!scanner.hasNextLine()) {
                System.out.println("No input, exiting");
                return;
            }
            try {
                id = Integer.parseInt(scanner.nextLine().trim());
                if (id < 0 || id >= Node.NODES) {
                    id = null;
                    System.out.println(Node.INPUT_ERROR);
                }
            } catch (NumberFormatException e) {
                System.out.println(Node.INPUT_ERROR);
            }
        }

        new Node(id, scanner);
    }
}
