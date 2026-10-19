package Nav;

import java.awt.image.BufferedImage;   // Imports BufferedImage for handling QR images

import java.io.FileWriter;  // Imports FileWriter to write logs to a file

import java.io.IOException; // Imports IOException for handling file-related errors

import java.time.LocalTime; // Imports LocalTime for time-related operations

import java.time.ZoneId;  // Imports ZoneId to specify time zone

import java.time.format.DateTimeFormatter; // Imports DateTimeFormatter to format time

import java.util.ArrayList;   // Imports ArrayList to store movement history

import swiftbot.Button;  // Imports Button class from SwiftBot API

import swiftbot.SwiftBotAPI;  // Imports SwiftBot API for controlling the robot

import java.util.StringTokenizer; // for multi-commands such as (F,50,4;R,50,5)


// Class to log robot actions
class Logfile {

    private final ArrayList<String> actions;  // Stores executed movement commands
    private final ArrayList<Integer> speeds;  // Stores speed values for each movement
    private final ArrayList<Integer> times;   // Stores duration values for each movement
    private final long start;   // Stores the program's start time
    
    public static void main(String[] args) throws InterruptedException, IOException {
        new Navigate().run();
    }

    // Constructor to initialise the Log-file
    public Logfile() {
        this.actions = new ArrayList<>();  // the list for actions
        this.speeds = new ArrayList<>();   //  the list for speeds
        this.times = new ArrayList<>();    //  the list for times
        this.start = System.currentTimeMillis();  // Record the program's start time
    }

    // Method to log an action with its speed and time
    public void logAction(String action, int speed, int time) {
        actions.add(action);  // Add the action to the actions list
        speeds.add(speed);    // Add the speed to the speeds list
        times.add(time);      // Add the time to the times list
    }

    // Method to save the log to a file
    public void SaveLogFile(String filename) throws IOException {
        FileWriter writer = null;  // Declare the FileWriter outside the try block
        try {
            writer = new FileWriter(filename);  // Create a FileWriter to write to the file

            // Get the current time in GMT and format it
            LocalTime now = LocalTime.now(ZoneId.of("GMT"));
            DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss");

            // Write the log basic description
            writer.write("Log at " + now.format(format) + "\n");
            writer.write("Total Execution Time: " + (System.currentTimeMillis() - start) / 1000 + " seconds\n\n");

            // Write each executed command with its speed and duration
            for (int i = 0; i < actions.size(); i++) {
                writer.write("Command: " + actions.get(i) + ", Speed: " + speeds.get(i) + ", Duration: " + times.get(i) + " seconds\n");
            }
        } finally {
            // Ensure the FileWriter is closed even if an exception occurs
            if (writer != null) {
                writer.close();
            }
        }
    }
}


// Interface for robot actions
interface Task8 {
  
void perform() throws InterruptedException;   // Method to execute a command
    
    void reverse() throws InterruptedException;   // Method to undo the command
}

// Action to move forward

class ForwardAction implements Task8 {
    private final SwiftBotAPI Swiftbot;  // API instance to control the robot
    private final int speed;   // Speed of movement
    int time;   // Duration of movement
    private final int moveDuration;  // Duration of movement in milliseconds

    public ForwardAction(SwiftBotAPI Swiftbot, int speed, int time) {
        this.Swiftbot = Swiftbot;
        this.speed = speed;
        this.time = time;
        this.moveDuration = time * 1000;  // Convert seconds to milliseconds
    }

    public void perform() throws InterruptedException {
        Swiftbot.move(speed, speed, moveDuration);  // Moves forward
    }

    public void reverse() throws InterruptedException {
        Swiftbot.move(-speed, -speed, moveDuration);  // Moves backward to undo for T command
    }
}

    
// Action to turn right
class RightTurnAction implements Task8 {
    private final SwiftBotAPI Swiftbot;
    private final int speed;
    private final int time;
    private final int turnDuration = 500;  // Fixed turn duration
    private final int turnRightSpeed = 100;  // Right turn speed
    private final int turnLeftSpeed = -100;  // Left turn speed

    public RightTurnAction(SwiftBotAPI Swiftbot, int speed, int time) {
        this.Swiftbot = Swiftbot;
        this.speed = speed;
        this.time = time;
    }

    public void perform() throws InterruptedException {
        Swiftbot.move(turnRightSpeed, turnLeftSpeed, turnDuration);  // Turns right
        Swiftbot.move(speed, speed, time * 1000);  // Moves forward
    }

    public void reverse() throws InterruptedException {
        Swiftbot.move(turnLeftSpeed, turnRightSpeed, turnDuration);  // Turns left to undo
        Swiftbot.move(-speed, -speed, time * 1000);  // Moves backward
    }
}

class LeftTurnAction implements Task8 {
    private final SwiftBotAPI Swiftbot;
    private final int speed;
    private final int time;
    private final int turnDuration = 500;  // Fixed turn duration
    private final int turnLeftSpeed = -100;  // Left turn speed
    private final int turnRightSpeed = 100;  // Right turn speed

    public LeftTurnAction(SwiftBotAPI Swiftbot, int speed, int time) {
        this.Swiftbot = Swiftbot;
        this.speed = speed;
        this.time = time;
    }

    public void perform() throws InterruptedException {
        Swiftbot.move(turnLeftSpeed, turnRightSpeed, turnDuration);  // Turns left
        Swiftbot.move(speed, speed, time * 1000);  // Moves forward
    }

    public void reverse() throws InterruptedException {
        Swiftbot.move(turnRightSpeed, turnLeftSpeed, turnDuration);  // Turns right to undo
        Swiftbot.move(-speed, -speed, time * 1000);  // Moves backward
    }
}

// action to move Backwards 
class BackwardAction implements Task8 {
    private final SwiftBotAPI Swiftbot;
    private final int speed;
    int time;
    private final int moveDuration;  // Duration for the backward movement

    public BackwardAction(SwiftBotAPI Swiftbot, int speed, int time) {
        this.Swiftbot = Swiftbot;
        this.speed = speed;
        this.time = time;
        this.moveDuration = time * 1000;  // Calculate duration in milliseconds
    }

    public void perform() throws InterruptedException {
        Swiftbot.move(-speed, -speed, moveDuration);  // Moves backward
    }

    public void reverse() throws InterruptedException {
        Swiftbot.move(speed, speed, moveDuration);   // Moves forward to undo
    }
}


class Userinput {
    public static Task8 errorhandling(String input, SwiftBotAPI Swiftbot) {
    
        String[] parts = input.split(",");  // Splits input command by commas
     

        if (parts.length < 3) {   // Ensures the command has sufficient parameters [0],[1],[2]... F,50,2
        
            throw new IllegalArgumentException("Invalid input format");  //error message 
        }
       
        String action = parts[0];  // Extracts the command identifier for e.g. F, B ,R ,L 
        
        int speed = Integer.parseInt(parts[1]);   // Extracts speed value
        
        int time = Integer.parseInt(parts[2]);   // Extracts duration value

        
     // Create and return the appropriate action
        return createAction(action, Swiftbot, speed, time);
    }

    private static Task8 createAction(String action, SwiftBotAPI Swiftbot, int speed, int time) {
        
        // Ensures speed and duration are within valid ranges
        if (speed < 0 || speed > 100 || time <= 0 || time > 6) {
            throw new IllegalArgumentException("Invalid speed or time");
        }

     // Returns the appropriate command based on the command type
        if ("F".equals(action)) {
            return new ForwardAction(Swiftbot, speed, time);
        } else if ("B".equals(action)) {
            return new BackwardAction(Swiftbot, speed, time);
        } else if ("R".equals(action)) {
            return new RightTurnAction(Swiftbot, speed, time);
        } else if ("L".equals(action)) {
            return new LeftTurnAction(Swiftbot, speed, time);
        } else {
            throw new IllegalArgumentException("Unknown command please try again: " + action);
        }
    }
}

// Main class to control the robot
public class Navigate {

    private final SwiftBotAPI Swiftbot;  // SwiftBot API instance for robot control
    
    private final Logfile logsaver;  // log-saver to store movements and commands
    
    private final ArrayList<Task8> actionHistory;  // Array-list to track executed commands
    
    // under-light signals is input or QR code is in correct format 
    private boolean isActive = true;
    private final int[] green = {0, 255, 0};  // RGB values for green light
    private final int[] red = {255, 0, 0};      // RGB values for red light
    private final int[] blue = {0, 0, 255};     // RGB values for blue light
    private final int[] yellow = {255, 255, 0};   // RGB values for yellow light
    private final java.util.Scanner scanner;        // Scanner for user input typing in terminal 
 
    public Navigate() {
       
    Swiftbot = new SwiftBotAPI();  // Initialises SwiftBot API
        
        logsaver = new Logfile();   // Initialises movement log
        
        actionHistory = new ArrayList<>();    // Initialises command history for T function 
        
        scanner = new java.util.Scanner(System.in);  // Initialises scanner for user input
        
        XandYButtons();
    }

    private void XandYButtons () {
    // Assigns Y button a function 
        Swiftbot.disableButton(Button.Y);
        Swiftbot.enableButton(Button.Y, this::YButton);
        
     // Assigns X button a function 
        Swiftbot.disableButton(Button.X);
        Swiftbot.enableButton(Button.X, this::XButton);
    }

   
    private void YButton() {
        try {
            scanQR();      // Processes QR code when Y button is pressed
        } catch (InterruptedException | IOException e) {   // error handling 
        System.err.println("Failed to process QR code, kindly try again" + e.getMessage()); //error messages highlights in red 
        
        }
    }

    // Handle X button press
    private void XButton() {
        try {
            stopProgram();    // Terminate programme when X button is pressed
        } catch (IOException e) {    // error handling 
        System.err.println("Failed to terminate program, kindly try again" + e.getMessage()); //error messages highlights in red 
        }
    }
    
    
    private void scanQR() throws InterruptedException, IOException {
        // Indicate that QR code scanning is in progress
        System.out.println("Scanning QR code...kindly be patient");
        Swiftbot.setButtonLight(Button.Y, true); // Turn on button light for visual feedback

        try {
            // Capture and decode the QR code image
            BufferedImage img = Swiftbot.getQRImage();
            if (img == null) {
                System.err.println("Failed to capture QR image! Check camera connection.");
                QRFailure();
                return;
            }

            // Decode the QR code image
            String qrCodeText = Swiftbot.decodeQRImage(img);

            // Check if a QR code was detected
            if (qrCodeText != null && !qrCodeText.isEmpty()) {
                System.out.println("QR code detected: " + qrCodeText);
                QRSuccess(); // Provide visual feedback for successful detection
                executeCommands(qrCodeText); // Execute commands from the QR code
            } else {
                System.out.println("No QR code found.");
                QRFailure(); // Provide visual feedback for failure
            }

        } catch (Exception e) {
            // Handle errors during scanning
            System.err.println("Error during QR code scanning: " + e.getMessage());
            e.printStackTrace();
            QRFailure(); // Provide visual feedback for errors
        } finally {
            // Turn off button light after scanning
            Swiftbot.setButtonLight(Button.Y, false);
        }
    }
    
    // Method to indicate successful QR code detection
    private void QRSuccess() throws InterruptedException {
        Swiftbot.fillUnderlights(green); // Turn on green under-lights
        Thread.sleep(500); // Wait for 500ms
        
        Swiftbot.disableUnderlights(); // Turn off under-lights
        Thread.sleep(500); // Wait for 500ms
    }

    
    // Method to indicate failure (no QR code or error)
    private void QRFailure() throws InterruptedException {
        Swiftbot.fillUnderlights(red); // Turn on red under-lights
        
        Thread.sleep(500); // Wait for 500ms
        Swiftbot.disableUnderlights(); // Turn off under-lights
    }

    
 // Method to execute commands from the QR code
    private void executeCommands(String commands) {
        System.out.println("Executing commands: " + commands);

        // Split multiple commands by semicolon
        String[] commandList = commands.split(";");

        for (String command : commandList) {
            try {
                // Execute each command
                Singlecommandinput(command);
            } catch (Exception e) {
                System.err.println("Failed to execute command: " + command);
                e.printStackTrace();
            }
        }
    }

    private void Singlecommandinput(String input) throws InterruptedException, IOException {
    
        String[] commands = input.split(","); // Split the input command string by commas
        String action = commands[0]; // The first element is the action (e.g., F, B, R, L)

        if (CommandValidation(action)) {
            HandleMovementCommand(commands, action);
        } else if (action.equals("T")) {
            HandleRetraceT(commands);
        } else if (action.equals("W")) {
            HandleLogSaveW(commands);
        } else {
            System.out.println("Unknown command: " + action);
        }
    }

 // Method to check if the action is a movement command (F, B, R, L)
    private boolean CommandValidation(String action) {
        return action.equals("F") || action.equals("B") || action.equals("R") || action.equals("L");
    }

    // Method to handle movement commands (F, B, R, L)
    private void HandleMovementCommand(String[] commands, String action) throws InterruptedException, IOException {
    
        try {
            // Validate command format
            if (commands.length < 3) {
                System.out.println("Invalid command format. Expected: action,speed,time");
                return;
            }

            // Set under-lights based on the action
            setUnderlights(action);


            // Log the movement details
            int speed = Integer.parseInt(commands[1]);
            int time = Integer.parseInt(commands[2]);
            logsaver.logAction(action, speed, time);
            
            // Create and execute the appropriate action
            
            Task8 navigate = createMovementTask(action, speed, time);
            navigate.perform();

            // Store the executed command in history
            actionHistory.add(navigate);

            // Log the movement details
            logsaver.logAction(action, speed, time);

            

            System.out.println("Movement completed!");

            Thread.sleep(500);
            Swiftbot.disableUnderlights();
            
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid command: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Invalid speed or time format. Please provide valid integers.");
        }
    }
    
    
 // Method to create a Task8 object based on the action
    private Task8 createMovementTask(String action, int speed, int time) {
    
        if (action.equals("F")) {
            return new ForwardAction(Swiftbot, speed, time);
        } else if (action.equals("B")) {
            return new BackwardAction(Swiftbot, speed, time);
        } else if (action.equals("R")) {
            return new RightTurnAction(Swiftbot, speed, time);
        } else if (action.equals("L")) {
            return new LeftTurnAction(Swiftbot, speed, time);
        } else {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }
    

    // Method to set under-lights based on the action
    private void setUnderlights(String action) {
        if (action.equals("F")) {
            Swiftbot.fillUnderlights(green);
        } else if (action.equals("B")) {
            Swiftbot.fillUnderlights(red);
        } else if (action.equals("R")) {
            Swiftbot.fillUnderlights(blue);
        } else if (action.equals("L")) {
            Swiftbot.fillUnderlights(yellow);
        }
    }

    // Method to handle the retrace command (T)
    private void HandleRetraceT(String[] commands) throws InterruptedException {
        if (commands.length == 2) {
            try {
                Swiftbot.fillUnderlights(green);
                Thread.sleep(200);
                Swiftbot.disableUnderlights();
                Thread.sleep(200);

                int moves = Integer.parseInt(commands[1]);
                RetraceActionsforT(moves);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number of moves. Please provide a valid integer.");
            }
        } else {
            System.out.println("Use: T,moves (e.g., T,2). Invalid format: " + commands[0]);
        }
    }

    // Method to handle the save log command (W)
    private void HandleLogSaveW(String[] commands) throws InterruptedException, IOException {
        if (commands.length == 1) {
            Swiftbot.fillUnderlights(blue);
            Thread.sleep(200);
            Swiftbot.disableUnderlights();
            Thread.sleep(200);

            logsaver.SaveLogFile("Navigate.txt");
            System.out.println("Log has been saved.");
        } else {
            System.out.println("Use: W (letter W only). Invalid format: " + commands[0]);
        }
    }

    private void Multicommandinput(String input) throws InterruptedException, IOException {
        // Create a StringTokenizer to split the input commands by a semicolon
        StringTokenizer tokenizer = new StringTokenizer(input, ";");

        // Loop through each command
        while (tokenizer.hasMoreTokens()) {
            String cmd = tokenizer.nextToken().trim(); // Get the next command and trim any extra spaces
            Singlecommandinput(cmd); // Execute the command
        }
    }


    public void run() throws InterruptedException, IOException {
        
    DisplayMenuInstructionsforUI();
        
        while (isActive) {
            System.out.println("\nWaiting for input...");
            System.out.println("1. Enter command manually (e.g., F,50,2...B,50,2...R,50,2...L,50,2...T,3...W)");
            System.out.println("2. Press Button Y to scan QR code");
            System.out.println("3. Type 'exit' or press X on SwiftBot to end program");

            if (scanner.hasNextLine()) {     // Check if user has entered input
            
                String input = scanner.nextLine().trim();   // Read input and remove extra spaces e.g. F,  50,3
                
                if (input.equals("exit")) {     // If 'exit' is entered, terminate the program
                    stopProgram();  
                    break;
                }
                try {
                    Multicommandinput(input);
                } catch (Exception e) {
                    System.err.println("Error");  //error messages highlights in red 
                }
            }
        }
    }

    private void DisplayMenuInstructionsforUI() {
    System.out.println(" ");
        System.out.println("      ******* TASK 8 *****       ");
        System.out.println("      *                   *      ");
        System.out.println("    *     O         O       *    ");
        System.out.println("   *        \\     //        *   ");
        System.out.println("  *          ------          *   ");
        System.out.println("  *                         *    ");
        System.out.println("   *      \\_______//      *     ");
        System.out.println("    *                   *        ");
        System.out.println("      *               *          ");
        System.out.println("        **************           ");
    
        System.out.println("**********Welcome to Task 8 Navigate!\n**********" +
                "Commands:\n" +
                "- F: Move Forward\n" +
                "- B: Move Backward\n" +
                "- R: Turn Right\n" +
                "- L: Turn Left\n" +
                "- T: Retrace actions\n" +
                "- W: Save log to file\n" +
                "Speed (0-100) and duration (1-6 seconds) must be specified.");

        System.out.println("\nOptions:");
        
        System.out.println("1. Scan QR Code");
        
        System.out.println("2. Enter command manually");
        
        System.out.println("3. Type 'exit' to end program");
       
        System.out.println("\nButtons:");
        
        System.out.println("Button Y: Scan QR Code");
        
        System.out.println("Button X: Stop Program");
    }
    
    
private void stopProgram() throws IOException {
    System.out.println("Task terminating. Hope you had a wonderful experience. Goodbye!");

    try {
        saveLogFile(); // Method for log saving
    } catch (IOException e) {
        System.err.println("Error saving log file: " + e.getMessage());
    }

    closeLogFile(); 
    isActive = false; // Mark program as inactive
}

// Method to save the log file
private void saveLogFile() throws IOException {
    logsaver.SaveLogFile("Navigate.txt");
    System.out.println("Log successfully saved!");
}

// Method to close scanner
private void closeLogFile() {
    if (scanner != null) {
        scanner.close();
    }
}

    private void RetraceActionsforT(int moves) throws InterruptedException {
        if (moves > actionHistory.size() ||  moves < 1) {
            System.out.println("Error: Too many moves to retrace.");
            return;
        }

        for (int i = 0; i < moves; i++) {
            int lastIndex = actionHistory.size() - 1;
            Task8 action = actionHistory.remove(lastIndex);  // Remove the last action and retrace it
            action.reverse();
        }
    }

}

