import java.util.Scanner;
import Traffic.TrafficLightNavigator;
import Tunnel.Tunnel_Vision;
import Search.SearchForLight;
import Draw.DrawShape;
import Zig.ZigZag;
import Nav.Navigate;
public class MainRunner {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input;

        while (true) {
            System.out.println("\n=== Menu ===");
            System.out.println("Type the task name:");
            System.out.println("search for light");
            System.out.println("Tunnel vision");
            System.out.println("navigate");
            System.out.println("draw shape");
            System.out.println("zigzag");
            System.out.println("traffic light");
            System.out.println("master mind");
            System.out.println("detect object");
            System.out.println("snakes and ladders");
            System.out.println("dance");
            System.out.println("exit to exit the program");
            System.out.print("Enter your choice: ");

            input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "traffic light":
                	new TrafficLightNavigator();
                   
                    break;
                case "tunnel vision":
                	new Tunnel_Vision();
                    
                    break;
                case "search for light":
                	new SearchForLight();
                    
                    break;
                case "draw shape":
                	new DrawShape();
                    
                    break;
                case "zigzag":
                	new ZigZag();
                    
                    break;
                case "navigate":
                	new Navigate();
                	break;
                case "":
                    
                    break;
                case "task8":
                    
                    break;
                case "task9":
                    
                    break;
                case "exit":
                    System.out.println("Exiting program...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid input. Please try again.");
            }
        }
    }
}

   
