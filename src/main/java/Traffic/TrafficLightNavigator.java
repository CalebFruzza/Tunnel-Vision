package Traffic;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import swiftbot.Button;
import swiftbot.ImageSize;
import swiftbot.SwiftBotAPI;


public class TrafficLightNavigator {
	// SwiftBot API stuff
    private SwiftBotAPI swiftBot;
    
    // tracks program state
    private boolean isRunning = false;
    private boolean isMoving = false;
    private boolean firstDetection = true;
    
    // speed
    private int initialSpeed = 40;
    
    // keeps track of light colours for the log
    private int redLightsDetected = 0;
    private int greenLightsDetected = 0;
    private int blueLightsDetected = 0;
    private long startTime;
    
    // sets up the robot and buttons
    public  TrafficLightNavigator() {
        System.out.println("Starting up...");
        try {
            swiftBot = new SwiftBotAPI();
            setupButtons();
            System.out.println("Ready! Press A to start.");
        } catch (Exception e) {
            System.out.println("Error starting: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // configures buttons
    private void setupButtons() {
        // A starts progrm
        swiftBot.enableButton(Button.A, () -> {
            if (!isRunning) {
                isRunning = true;
                startProgram();
            }
        });
        
        // X stops 
        swiftBot.enableButton(Button.X, () -> {
            if (isRunning) {
                stopProgram();
            } else {
                System.out.println("Already stopped. Exiting...");
                System.exit(0);
            }
        });
        
        // Y shows the stats
        swiftBot.enableButton(Button.Y, () -> {
            if (!isRunning) {
                displayLog();
            }
        });
    }
    
    // starts the program when A is pressed
    private void startProgram() {
        System.out.println("Starting navigation program...");
        startTime = System.currentTimeMillis();
        
        // yellow lights mean we're ready
        swiftBot.fillUnderlights(new int[]{255, 255, 0});
        
        System.out.println("Speed set to: " + initialSpeed);
        
        // waiting for first traffic light
        System.out.println("Standing by for traffic lights...");
        
        // run the main loop in background
        new Thread(() -> {
            runMainLoop();
        }).start();
    }
    
    // main program loop
    private void runMainLoop() {
        while (isRunning) {
            try {
                checkForTrafficLights();
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                System.out.println("Got interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Oops: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    // looks for traffic lights using camera
    private void checkForTrafficLights() {
        try {
            // see how far away objects are
            double distance = swiftBot.useUltrasound();
            
            // only check for traffic lights within 30cm
            if (distance <= 30.0) {
                System.out.println("Found something: " + distance + " cm away");
                
                // take a picture
                BufferedImage image = swiftBot.takeStill(ImageSize.SQUARE_240x240);
                
                if (image != null) {
                    // figure out what colour it is
                    String colour = determineColour(image);
                    
                    System.out.println("Detected a " + colour + " light at " + distance + " cm");
                    
                    // do something based on the colour
                    respondToTrafficLight(colour);
                } else {
                    System.out.println("Camera didn't work");
                }
            } else if (!firstDetection && !isMoving) {
                // keep going if we're not moving
                System.out.println("Nothing ahead, moving forward");
                swiftBot.startMove(initialSpeed, initialSpeed);
                isMoving = true;
            }
        } catch (Exception e) {
            System.out.println("Problem checking lights: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // figures out what colour is in the image
    private String determineColour(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        long totalR = 0, totalG = 0, totalB = 0;
        int pixels = 0;
        
        // get average RGB values
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                totalR += r;
                totalG += g;
                totalB += b;
                pixels++;
            }
        }
        
        int avgR = (int) (totalR / pixels);
        int avgG = (int) (totalG / pixels);
        int avgB = (int) (totalB / pixels);
        
        System.out.println("RGB values: (" + avgR + ", " + avgG + ", " + avgB + ")");
        
        // need to play with these values to get them right
        if (avgR > avgG + 20 && avgR > avgB + 20 && avgR > 100) {
            redLightsDetected++;
            return "red";
        } else if (avgG > avgR + 20 && avgG > avgB + 20 && avgG > 100) {
            greenLightsDetected++;
            return "green";
        } else if (avgB > avgR + 20 && avgB > avgG + 20 && avgB > 100) {
            blueLightsDetected++;
            return "blue";
        } else {
            return "unknown";
        }
    }
    
    // handles the different traffic light colours
    private void respondToTrafficLight(String colour) {
        try {
           
            if (firstDetection) {
                firstDetection = false;
                System.out.println("First light found!");
            }
            
            switch (colour) {
                case "red":
                    // red means stop and stay stopped
                    swiftBot.fillUnderlights(new int[]{255, 0, 0});
                    System.out.println("RED: stopping until green or blue light detected");
                    
                    swiftBot.stopMove();
                    isMoving = false;
                    
                    // Don't continue after red - wait for green or blue
                    break;
                    
                case "green":
                    // green means go
                    swiftBot.fillUnderlights(new int[]{0, 255, 0});
                    System.out.println("GREEN: moving forward");
                    
                    // pass through quickly
                    swiftBot.move(initialSpeed, initialSpeed, 2000);
                    
                    swiftBot.stopMove();
                    isMoving = false;
                    Thread.sleep(1000);
                    
                    // continue afterwards
                    swiftBot.fillUnderlights(new int[]{255, 255, 0});
                    swiftBot.startMove(initialSpeed, initialSpeed);
                    isMoving = true;
                    break;
                    
                case "blue":
                    // blue means yield
                    swiftBot.stopMove();
                    isMoving = false;
                    System.out.println("BLUE: yielding");
                    Thread.sleep(1000);
                    
                    // blink
                    blinkUnderlights(new int[]{0, 0, 255}, 3);
                    
                    // Turn right 90 degrees
                    System.out.println("Turning right 90 degrees");
                    swiftBot.move(initialSpeed, -initialSpeed, calculateTurnTime(45)); //may need to change this value depending on surface 
                    // Move forward about 10cm
                    System.out.println("Moving forward about 10cm");
                    swiftBot.move(initialSpeed / 2, initialSpeed / 2, 3000);
                    
                    // Stop 
                    swiftBot.stopMove();
                    Thread.sleep(1000);
                    
                    // Turn left 90 degrees 
                    System.out.println("Turning left 90 degrees to return to path");
                    swiftBot.move(-initialSpeed, initialSpeed, calculateTurnTime(45));
                    
                    // continue 
                    swiftBot.fillUnderlights(new int[]{255, 255, 0});
                    swiftBot.startMove(initialSpeed, initialSpeed);
                    isMoving = true;
                    break;
                    
                default:
                    // when it cant tell the colour 
                    System.out.println("Not sure what colour this is");
                    if (firstDetection) {
                        // start moving anyway if it's first detection
                        swiftBot.startMove(initialSpeed, initialSpeed);
                        isMoving = true;
                        firstDetection = false;
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("Problem responding to light: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    //lights blink
    private void blinkUnderlights(int[] colour, int times) {
        try {
            for (int i = 0; i < times; i++) {
                swiftBot.fillUnderlights(colour);
                Thread.sleep(300);
                swiftBot.disableUnderlights();
                Thread.sleep(300);
            }
        } catch (InterruptedException e) {
            System.out.println("Blinking interrupted: " + e.getMessage());
        }
    }
    
    
    private int calculateTurnTime(int degrees) {
       
        return degrees * 10; //how long in ms each dgree is 
    }
    
    // X to stop
    private void stopProgram() {
        System.out.println("Stopping...");
        isRunning = false;
        swiftBot.stopMove();
        isMoving = false;
        swiftBot.disableUnderlights();
        
        System.out.println("Press Y to see stats, X to exit");
    }
    
    // shows the stats when Y is pressed
    private void displayLog() {
        // figure out which light it saw most
        String mostFrequent = "None";
        int maxCount = 0;
        int totalLights = redLightsDetected + greenLightsDetected + blueLightsDetected;
        
        if (redLightsDetected > maxCount) {
            mostFrequent = "Red";
            maxCount = redLightsDetected;
        }
        if (greenLightsDetected > maxCount) {
            mostFrequent = "Green";
            maxCount = greenLightsDetected;
        }
        if (blueLightsDetected > maxCount) {
            mostFrequent = "Blue";
            maxCount = blueLightsDetected;
        }
        
        // figure out how long it ran
        long executionTime = System.currentTimeMillis() - startTime;
        String executionTimeStr = String.format("%d minutes, %d seconds", 
                                executionTime / 60000, (executionTime % 60000) / 1000);
        
        // print the stats
        System.out.println("\n--------- Results ---------");
        System.out.println("Total lights seen: " + totalLights);
        System.out.println("Red lights: " + redLightsDetected);
        System.out.println("Green lights: " + greenLightsDetected);
        System.out.println("Blue lights: " + blueLightsDetected);
        System.out.println("Most common: " + mostFrequent + 
                         " (" + maxCount + " times)");
        System.out.println("Total time: " + executionTimeStr);
        System.out.println("--------------------------\n");
        
        // save to a file
        writeLogToFile(mostFrequent, maxCount, totalLights, executionTimeStr);
    }
    
    // saves the stats to a file
    private void writeLogToFile(String mostFrequent, int maxCount, int totalLights, String executionTimeStr) {
        try {
            // filename with timestamp 
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
            String timestamp = dateFormat.format(new Date());
            String filename = "traffic_log_" + timestamp + ".txt";
            
            FileWriter writer = new FileWriter(filename);
            
            writer.write("--------- Traffic Light Log ---------\n");
            writer.write("Date: " + new Date().toString() + "\n\n");
            writer.write("Total lights: " + totalLights + "\n");
            writer.write("Red: " + redLightsDetected + "\n");
            writer.write("Green: " + greenLightsDetected + "\n");
            writer.write("Blue: " + blueLightsDetected + "\n");
            writer.write("Most common: " + mostFrequent + 
                       " (" + maxCount + " times)\n");
            writer.write("Total time: " + executionTimeStr + "\n");
            writer.write("----------------------------------\n");
            
            writer.close();
            
            File logFile = new File(filename);
            System.out.println("Log saved to: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Couldn't save log: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // launches the program
    public static void main(String[] args) {
        new TrafficLightNavigator();
    }
}


