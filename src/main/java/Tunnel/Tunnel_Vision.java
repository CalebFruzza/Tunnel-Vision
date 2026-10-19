package Tunnel;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import swiftbot.Button;
import swiftbot.ImageSize;
import swiftbot.SwiftBotAPI;

public class Tunnel_Vision {
    private static final SwiftBotAPI swiftBot = new SwiftBotAPI();
    private static volatile boolean running = true;//control variables for program
    private static volatile boolean Started = false;
    private static volatile boolean Completed = false;
    //constants for obstacle detection, movement and light intensity
    private static final int Obstacle_Threshold = 40; //minimum distance
    private static final int Image_Interval = 500;//every 500 miliseconds 
    private static final int slow_Speed = 35;
    private static final double Lightintensity_threshold = 0.3; 
    private static final double Distanceperinterval = 2.5; 
    private static final int Max_Tunnels = 3;
    private static final int calibration_delay = 1000;
    private static final String Logfilepass = "tunnel_vision_log.txt";
    private static final String Imagedirectory = "."; 
    public static void main(String[] args) {
        startupscreen();//runs the function startupscreen
        setup_Buttons();//runs the function for setting up buttons
        
        //keeps the program running until stopped 
        while (running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("Program interrupted: " + e.getMessage());
                break;
            }
        }
    }
    //displays instructions on how to start up the program
    private static void startupscreen() {
        System.out.println("Task 5");
        System.out.println("********************");
        System.out.println("Press 'Y' to start.");
        System.out.println("Press 'X' to exit.");
    }
    
    private static void setup_Buttons() {
        
        swiftBot.disableAllButtons();
        
        //enables the button Y to start the program 
        swiftBot.enableButton(Button.Y, () -> {
            if (!Started && !Completed) {
                Started = true;
                TunnelDetection();
            } else if (Completed) {
                display_Log();
            }
        });
        
        //enables the button X to terminate the program
        swiftBot.enableButton(Button.X, () -> {
            System.out.println("Stopping the program...");
            running = false;
            swiftBot.stopMove();
            swiftBot.disableAllButtons();
            System.exit(0);
        });
    }
    
    private static void TunnelDetection() {
        System.out.println("Starting tunnel detection...");//outputs the message
        swiftBot.fillUnderlights(new int[]{255, 0, 0});//base underlight colour set to red
        
        long startTime = System.currentTimeMillis();
        int tunnelCount = 0;
        double Distance = 0;
        boolean insideTunnel = false;
        double tunnelStartDistance = 0;
        double tunnelEndDistance = 0;
        double[] tunnelLengths = new double[ Max_Tunnels];
        double[] tunnelLightIntensities = new double[ Max_Tunnels];
        double[] distancesBetweenTunnels = new double[ Max_Tunnels - 1];
        double TunnelEndDistance = 0;
        double baselineIntensity = 0;
        
      
        try {
            System.out.println("Calibrating light intensity...");
            BufferedImage initialImage = swiftBot.takeStill(ImageSize.SQUARE_480x480);
            baselineIntensity = calculateLightIntensity(initialImage);
            System.out.println("Baseline light intensity: " + String.format("%.2f", baselineIntensity));
            
            
            Thread.sleep(calibration_delay);
        } catch (Exception e) {
            System.out.println("Error getting baseline light intensity: " + e.getMessage());
            return;
        }
        //main loop for detecting tunnels
        while (running && tunnelCount <  Max_Tunnels) {
            try {
                //check for obstacles within 40cm
                double distanceToObject = swiftBot.useUltrasound();
                if (distanceToObject < Obstacle_Threshold) {
                    handleObstacle(distanceToObject);
                    break;
                }
                
                //move slow and capture the image 
                swiftBot.move( slow_Speed, slow_Speed, Image_Interval);
                
               
                BufferedImage image = swiftBot.takeStill(ImageSize.SQUARE_480x480);
                if (image == null) {
                    System.out.println("Error: Failed to capture image");
                    break;
                }
                
                double currentIntensity = calculateLightIntensity(image);
                
                //detecting entering a tunnel
               
                if (!insideTunnel && currentIntensity < (baselineIntensity * (1 - Lightintensity_threshold))) {
                    insideTunnel = true;
                    tunnelStartDistance = Distance;
                    System.out.println("\nTunnel detected! Entering...");
                    System.out.println("Tunnel " + (tunnelCount + 1));
                    System.out.println("Tunnel entrance at: " + String.format("%.2f", tunnelStartDistance) + " cm");
                    swiftBot.fillUnderlights(new int[]{0, 255, 0}); 
                } else if (insideTunnel && currentIntensity > (baselineIntensity * (1 - Lightintensity_threshold))) {
                    insideTunnel = false;
                    tunnelEndDistance = Distance;
                    double tunnelLength = tunnelEndDistance - tunnelStartDistance;
                    tunnelLengths[tunnelCount] = tunnelLength;
                    tunnelLightIntensities[tunnelCount] = calculateAverageLightIntensity(image);
                    //detecting exiting a tunnel
                    if (tunnelCount > 0) {
                        distancesBetweenTunnels[tunnelCount - 1] = tunnelStartDistance - TunnelEndDistance ;
                        System.out.println("Distance from previous tunnel: " + String.format("%.2f", distancesBetweenTunnels[tunnelCount - 1]) + " cm");
                    }
                   TunnelEndDistance = tunnelEndDistance;
                    System.out.println("\nExiting tunnel...");
                    System.out.println("- Light intensity increased");
                    System.out.println("- Tunnel length: " + String.format("%.2f", tunnelLength) + " cm");
                    System.out.println("- Tunnel exit at: " + String.format("%.2f", tunnelEndDistance) + " cm");
                    System.out.println("- Moving to next tunnel...");
                    tunnelCount++;
                    swiftBot.fillUnderlights(new int[]{255, 0, 0}); 
                }
                
              
                Distance += Distanceperinterval ;
                
            } catch (Exception e) {
                System.out.println("Error during tunnel detection: " + e.getMessage());
                break;
            }
        }
        
        
        swiftBot.stopMove();
        
        //log results 
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        
        
        logResults(tunnelCount, tunnelLengths, tunnelLightIntensities, distancesBetweenTunnels, Distance, duration);
        
        
        Completed = true;
        
        
        System.out.println("\nPress button 'Y' to view the log or 'X' to exit.");
    }
    
    private static void handleObstacle(double distanceToObject) {
        System.out.println("\nObstacle detected at " + String.format("%.2f", distanceToObject) + " cm!");//outputds the objects distance from the bot
        swiftBot.fillUnderlights(new int[]{0, 0, 255});//turns blue 
        System.out.println("Taking photo and stopping program...");
        
        try {
            BufferedImage image = swiftBot.takeStill(ImageSize.SQUARE_480x480);
            if (image != null) {
                String obstacleFilename = "obstacle_" + System.currentTimeMillis() + ".png";
                File outputFile = new File(Imagedirectory, obstacleFilename);
                
                try {
                    ImageIO.write(image, "PNG", outputFile);
                    System.out.println("Obstacle photo saved to: " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("Warning: Could not save obstacle image: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Error capturing obstacle image: " + e.getMessage());
        }
        
        Completed = true;
        System.out.println("Press button 'Y' to view the log or 'X' to exit.");
    }
    
    private static double calculateLightIntensity(BufferedImage image) {
    	//calculates light intensity 
        if (image == null) return 0;
        
        int width = image.getWidth();
        int height = image.getHeight();
        double totalLight = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                totalLight += (red + green + blue) / 3.0;
            }
        }
        
        return totalLight / (width * height);
    }
    
    private static double calculateAverageLightIntensity(BufferedImage image) {
        return calculateLightIntensity(image);
    }
    
    private static void logResults(int tunnelCount, double[] tunnelLengths, double[] tunnelLightIntensities,
                                 double[] distancesBetweenTunnels, double totalDistance, double duration) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Logfilepass))) {
        	//writes all data to a txt file
            writer.write("Tunnel Vision Log\n");
            writer.write("=================\n\n");
            writer.write("Total number of tunnels: " + tunnelCount + "\n\n");
            
        
            for (int i = 0; i < tunnelCount; i++) {
                writer.write("Tunnel " + (i + 1) + ":\n");
                writer.write("  Length: " + String.format("%.2f", tunnelLengths[i]) + " cm\n");
                writer.write("  Start Position: " + String.format("%.2f", (i > 0 ? tunnelLengths[i-1] + distancesBetweenTunnels[i-1] : 0)) + " cm\n");
                writer.write("  End Position: " + String.format("%.2f", (i > 0 ? tunnelLengths[i-1] + distancesBetweenTunnels[i-1] + tunnelLengths[i] : tunnelLengths[i])) + " cm\n");
                writer.write("  Average Light Intensity: " + String.format("%.2f", tunnelLightIntensities[i]) + "\n");
                if (i < tunnelCount - 1) {
                    writer.write("  Distance to next tunnel: " + String.format("%.2f", distancesBetweenTunnels[i]) + " cm\n");
                }
                writer.write("\n");
            }
            
            
            writer.write("Distance Analysis:\n");
            writer.write("------------------\n");
            double totalTunnelLength = 0;
            for (int i = 0; i < tunnelCount; i++) {
                totalTunnelLength += tunnelLengths[i];
            }
            writer.write("Total tunnel length: " + String.format("%.2f", totalTunnelLength) + " cm\n");
            
            double totalGapDistance = 0;
            for (int i = 0; i < tunnelCount - 1; i++) {
                totalGapDistance += distancesBetweenTunnels[i];
            }
            writer.write("Total distance between tunnels: " + String.format("%.2f", totalGapDistance) + " cm\n");
            
           
            writer.write("\nSummary:\n");
            writer.write("---------\n");
            writer.write("  Total distance travelled: " + String.format("%.2f", totalDistance) + " cm\n");
            writer.write("  Duration of execution: " + String.format("%.2f", duration) + " seconds\n");
            writer.write("  Average speed: " + String.format("%.2f", totalDistance/duration) + " cm/s\n");
            writer.write("  Obstacle detected: " + (swiftBot.useUltrasound() < Obstacle_Threshold) + "\n");
            writer.write("  Log file saved to: " + Logfilepass + "\n");
            
            System.out.println("Log file saved successfully to: " + Logfilepass);
        } catch (IOException e) {
            System.out.println("Error writing to log file: " + e.getMessage());
        }
    }
    //displays log after user has terminated the code.
    private static void display_Log() {
        System.out.println("\nTunnel Vision Log");
        System.out.println("=================");
        try {
            java.nio.file.Path logPath = java.nio.file.Paths.get(Logfilepass);
            java.util.List<String> lines = java.nio.file.Files.readAllLines(logPath);
            for (String line : lines) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading log file: " + e.getMessage());
        }
        System.out.println("\nPress button 'X' to exit.");
    }
} 
