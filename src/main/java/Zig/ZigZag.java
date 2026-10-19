package Zig;


import swiftbot.*;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class ZigZag {

    static SwiftBotAPI swiftBot;
    static boolean restart = false;
    static String logFilePath = "/data/home/pi/zigzag_log.txt";
    static String longestJourneyDetails = "";
    static String shortestJourneyDetails = "";
    static Random random = new Random();
    static int journeyCount = 0;
    static double longestDistance = 0;
    static double shortestDistance = Double.MAX_VALUE;

    public static void main(String[] args) {
        try {
            swiftBot = new SwiftBotAPI();
        } catch (Exception e) {
            System.out.println("Failed to initialize SwiftBot");
            System.exit(1);
        }

        while (true) {
            // Prompt for a QR
            System.out.println("Please scan a QR code with the format 'Length:NumberOfSections' (e.g., '20:6').");

            // Scan it
            String qrInput = scanQR();
            if (qrInput == null || qrInput.isEmpty()) {
                showError();
                continue;
            }

            // Parse so int
            String[] values;
            int length, sections;
            
            try {
                values = qrInput.split(":");
                if (values.length != 2) {
                    showError();
                    continue;
                }

                length = Integer.parseInt(values[0]);
                sections = Integer.parseInt(values[1]);
            } catch (NumberFormatException e) {
                showError();
                continue;
            }

            // Validate
            if (!isValid(length, sections)) {
                showError();
                continue;
            }

            // Screen 1: Introduction
            showIntro(length, sections);

            // Screen 2: Current Movement
            int speed = random.nextInt(61) + 40; // Random speed between 40-100
            showMovement(length, sections, speed);

            // Perform the movement
            moveRobot(length, sections, speed);

            // Screen 3: Summary
            showSummary(length, sections, speed);

            // Exit or scan prompt
            System.out.println("\nPress 'Y' to scan another QR code or 'X' to exit.");

            // Reset flag
            restart = false;

            // Set up listeners
            swiftBot.enableButton(Button.Y, () -> {
                System.out.println("\nRestarting...");
                restart = true; // Flag true
                swiftBot.disableButton(Button.Y);
            });
            swiftBot.enableButton(Button.X, () -> {
                System.out.println("\nJourney Statistics:");
                System.out.println("Total number of zigzag journeys completed: " + journeyCount);
                if (journeyCount > 0) {
                    System.out.println("Longest journey: " + longestJourneyDetails);
                    System.out.println("Shortest journey: " + shortestJourneyDetails);
                    System.out.println("Log file location: " + logFilePath);
                }
                System.out.println("\nThanks for using SwiftBot Zigzag!");
                System.exit(0);
            });

            // Wait for button press
            while (!restart) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void showIntro(int length, int sections) {
        System.out.println("\n\n=============================");
        System.out.println(" SwiftBot Zigzag");
        System.out.println("=============================");
        System.out.println("\nThese are your current zigzag journey details (Length: Sections):");
        System.out.println("- Length: " + length + " cm");
        System.out.println("- Sections: " + sections);
        System.out.println("\nPress Enter to start the journey!");
        System.out.println("\n=============================\n\n");
    }

    private static void showMovement(int length, int sections, int speed) {
        System.out.println("\n\n=============================");
        System.out.println(" SwiftBot Movement");
        System.out.println("---------------------------------------------");
        System.out.println("\nJourney Starting...");
        System.out.println("- Length: " + length + " cm");
        System.out.println("- Sections: " + sections);
        System.out.println("- Speed: " + speed + "%");
        System.out.println("\n------------------------------------------------");

        // Display outward journey
        System.out.println("\nOutward Journey:");
        for (int i = 0; i < sections; i++) {
            String ledColor = (i % 2 == 0) ? "Green" : "Blue";
            System.out.println("Section " + (i+1) + ": Moving forward " + length + " cm... [LED: " + ledColor + "]");
            
            // If not the last section, turn
            if (i < sections - 1) {
                String turn = (i % 2 == 0) ? "right" : "left";
                System.out.println("Turning " + turn + "... [LED: " + ledColor + "]");
            }
        }
        
        // Display return journey
        System.out.println("\nTurning 180 degrees...");
        System.out.println("\nReturn Journey:");
        for (int i = sections-1; i >= 0; i--) {
            String ledColor = (i % 2 == 0) ? "Green" : "Blue";
            System.out.println("Section " + (i+1) + ": Moving forward " + length + " cm... [LED: " + ledColor + "]");
            
            // If not the first section, turn
            if (i > 0) {
                String turn = (i % 2 == 1) ? "right" : "left";
                System.out.println("Turning " + turn + "... [LED: " + ledColor + "]");
            }
        }

        System.out.println("\n=============================\n\n");
    }

    private static void showSummary(int length, int sections, int speed) {
        double totalDistance = length * sections;
        double timePerSection = (length * 10000.0) / (speed * 1.0) / 1000.0; // Time in seconds
        double totalTime = timePerSection * sections * 2; // Forward and return journey
        double straightLineDistance = calculateStraightDistance(length, sections);
        int numberOfTurns = sections - 1; // One less turn than sections

        System.out.println("\n\n=============================");
        System.out.println(" Journey Summary");
        System.out.println("=============================");
        System.out.println("\n- Total Distance: " + totalDistance + " cm");
        System.out.println("- Total Time Taken: " + String.format("%.2f", totalTime) + " seconds");
        System.out.println("- Straight-Line Distance: " + String.format("%.2f", straightLineDistance) + " cm");
        System.out.println("- Speed: " + speed + "%");
        System.out.println("- Number of turns: " + numberOfTurns);
        System.out.println("\n-------------------------------------------");
        System.out.println("\nThank you for playing!!!");
        System.out.println("Your journey log file is now stored on the swiftbot at:");
        System.out.println(logFilePath);
        System.out.println("\nPress:");
        System.out.println(" 'Y' to scan another QR code");
        System.out.println(" 'X' to exit.");
        System.out.println("\n=============================\n\n");
    }

    private static void showError() {
        System.out.println("\n\n=============================");
        System.out.println(" Error: Input values are invalid");
        System.out.println("=============================");
        System.out.println("\nPlease ensure that the input values meet the following requirements:");
        System.out.println("- Length must be between 15 and 85 cm.");
        System.out.println("- The number of sections must be an even number.");
        System.out.println("- The number of sections must not exceed 12.");
        System.out.println("\n----------------------------------------------");
        System.out.println("\nPlease try again.");
        System.out.println("\n=============================\n\n");
    }

    private static boolean isValid(int length, int sections) {
        return length >= 15 && length <= 85 && sections % 2 == 0 && sections <= 12;
    }

    private static void moveRobot(int length, int sections, int speed) {
        try {
            long startTime = System.currentTimeMillis();

            // Outward zigzag movement
            for (int i = 0; i < sections; i++) {
                // Set LED color (green for odd sections, blue for even)
                int[] color = (i % 2 == 0) ? new int[]{0, 255, 0} : new int[]{0, 0, 255}; // Green and Blue
                swiftBot.fillUnderlights(color);

                // Move forward for the section length
                moveForward(length, speed);
                swiftBot.stopMove(); // Stop
                Thread.sleep(1000); // Wait 1 second

                // Turn if not the last section
                if (i < sections - 1) {
                    if (i % 2 == 0) {
                        turnRight(); // Turn right after first, third, etc. sections
                    } else {
                        turnLeft(); // Turn left after second, fourth, etc. sections
                    }
                    swiftBot.stopMove(); // Stop
                    Thread.sleep(1000); // Wait 1 second
                }
            }

            // Calculate metrics before return journey
            double totalDistance = length * sections;
            double straightDistance = calculateStraightDistance(length, sections);

            // Turn around to start return journey
            turnAround();
            swiftBot.stopMove(); // Stop
            Thread.sleep(1000); // Wait 1 second

            // Retrace path back to starting position
            for (int i = sections - 1; i >= 0; i--) {
                // Set LED color (same as outward journey for each section)
                int[] color = (i % 2 == 0) ? new int[]{0, 255, 0} : new int[]{0, 0, 255}; // Green and Blue
                swiftBot.fillUnderlights(color);

                // Move forward for the section length
                moveForward(length, speed);
                swiftBot.stopMove(); // Stop
                Thread.sleep(1000); // Wait 1 second

                // Turn if not the first section
                if (i > 0) {
                    if (i % 2 == 1) {
                        turnRight(); // Turn right after section that was originally turned left
                    } else {
                        turnLeft(); // Turn left after section that was originally turned right
                    }
                    swiftBot.stopMove(); // Stop
                    Thread.sleep(1000); // Wait 1 second
                }
            }

            // Turn off underlights when complete
            swiftBot.disableUnderlights();

            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            // Log the journey details
            logJourney(length, sections, speed, totalDistance, duration, straightDistance);

            // Update longest/shortest journey records
            if (straightDistance > longestDistance) {
                longestDistance = straightDistance;
                longestJourneyDetails = "Length: " + length + " cm, Sections: " + sections + ", Straight Line Distance: " + String.format("%.2f", straightDistance) + " cm";
            }
            if (straightDistance < shortestDistance && straightDistance > 0) {
                shortestDistance = straightDistance;
                shortestJourneyDetails = "Length: " + length + " cm, Sections: " + sections + ", Straight Line Distance: " + String.format("%.2f", straightDistance) + " cm";
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void moveForward(int length, int speed) throws InterruptedException {
        double time = (length * 10000.0) / (speed * 1.0);
        swiftBot.move(speed, speed, (int) time); // Move
    }

    private static void turnRight() throws InterruptedException {
        swiftBot.move(70, -75, 1000); // Turns right at higher speed
    }

    private static void turnLeft() throws InterruptedException {
        swiftBot.move(-75, 70, 1000); // Turn left at higher speed
    }

    private static void turnAround() throws InterruptedException {
        // Perform two 90-degree turns to make a 180-degree turn
        turnRight();
        Thread.sleep(500);
        turnRight();
    }

    private static double calculateStraightDistance(int length, int sections) {
        // For a zigzag pattern with equal length segments:
        // If sections = 2: We move east, then north, so end position is (length, length)
        // If sections = 4: We move east, north, west, north, so end position is (0, 2*length)
        // And so on...
        
        // Calculate the x and y coordinates of the end point
        double x = 0;
        double y = 0;
        
        // Direction: 0=East, 1=North, 2=West, 3=South
        int direction = 0;
        
        for (int i = 0; i < sections; i++) {
            switch (direction) {
                case 0: // East
                    x += length;
                    break;
                case 1: // North
                    y += length;
                    break;
                case 2: // West
                    x -= length;
                    break;
                case 3: // South
                    y -= length;
                    break;
            }
            
            // After each section (except the last), turn 90 degrees
            if (i < sections - 1) {
                // Even sections turn right (clockwise), odd sections turn left (counter-clockwise)
                if (i % 2 == 0) {
                    direction = (direction + 1) % 4; // Turn right (clockwise)
                } else {
                    direction = (direction + 3) % 4; // Turn left (counter-clockwise)
                }
            }
        }
        
        // Use Pythagorean theorem to calculate straight-line distance
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    private static void logJourney(int length, int sections, int speed, double totalDistance, double duration, double straightDistance) { // File logging
        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            writer.write("Journey " + (++journeyCount) + ":\n");
            writer.write("Length: " + length + " cm, Sections: " + sections + "\n");
            writer.write("Speed: " + speed + "%\n");
            writer.write("Total Distance: " + totalDistance + " cm\n");
            writer.write("Duration: " + duration + " seconds\n");
            writer.write("Straight Line Distance: " + straightDistance + " cm\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String scanQR() {
        try {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + 10000; // 10 seconds to scan QR code

            while (System.currentTimeMillis() < endTime) {
                BufferedImage img = swiftBot.getQRImage();

                // Decode
                String decodedMessage = swiftBot.decodeQRImage(img);

                // Return
                if (!decodedMessage.isEmpty()) {
                    return decodedMessage;
                }

                // Wait before repeat
                Thread.sleep(1000);
            }

            // Return null for no QR
            System.out.println("No QR code detected within 10 seconds.");
            return null;
        } catch (Exception e) {
            // Exception handling
            System.out.println("An error occurred while scanning the QR code.");
            e.printStackTrace();
            return null;
        }
    }
}