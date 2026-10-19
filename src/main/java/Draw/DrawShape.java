package Draw;

import swiftbot.*;
import java.util.Scanner;
import java.awt.image.BufferedImage;

public class DrawShape {
    private static SwiftBotAPI swiftBot;
    private static String QRstr;

    // Constants for movement and turning
    private static final int timemovedpercm = 100;
    private static final int turnby120degrees = 600;
    private static final int turtimesquare = 650;
    private static final double travelfactor = 0.6;
    private static double circlefactor = 0.4;
    private static final int movingbackdistance = 15;
    private static double lastSideScaleFactor = 1.45;

    // Logging variables to track shapes drawn
    private static String shapeLog = "";
    private static double largestArea = 0;
    private static String largestShape = "";
    private static int squareCount = 0;
    private static int triangleCount = 0;
    private static int circleCount = 0;
    private static double totalTime = 0;
    private static int shapeCount = 0;

    public static void main(String[] args) {
        try {
            swiftBot = new SwiftBotAPI();
            Scanner reader = new Scanner(System.in);

            // Displays welcome message and instructions
            System.out.println("\n************************");
            System.out.println(" WELCOME TO DRAW SHAPE!");
            System.out.println("************************\n");
            System.out.println("Instructions:");
            System.out.println("1. Scan a QR code containing shape information (e.g., S-16 for a square, T-16-30-24 for a triangle or C-20 for a Circle).");
            System.out.println("2. The SwiftBot will draw the specified shape.");
            System.out.println("3. Type 'Y' to scan another QR code or the X button to exit.");
            System.out.println("\nPress any key to start...");
            reader.nextLine();

            enableButtons();

            while (true) {
                GetQRcodeInput(); // Gets the QR code input from the user

                if (validateShape(QRstr)) {
                    drawMultipleShapes(QRstr);
                    System.out.println("\nShape Drawing Completed");
                    System.out.println("Type 'Y' to scan another QR code or the X button to exit...");

                    String choice = reader.nextLine();
                    if (choice.equalsIgnoreCase("Y")) {
                        continue;
                    } else {
                        System.out.println("Invalid input. Please type 'Y' to continue or the X button to exit.");
                    }
                } else {
                    System.out.println("\nPlease scan a valid QR code.");
                    System.out.println("Press any key to try again...");
                    reader.nextLine();
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    // Enables the X button to exit the program
    private static void enableButtons() {
        swiftBot.enableButton(Button.X, () -> {
            System.out.println("\nThank You for playing Draw Shape!");
            displayDrawingLog();
            saveLog();
            System.exit(0);
        });
    }

    // Get QR code input from the user
    public static void GetQRcodeInput() {
        System.out.println("You have 10s to display a QR code");
        long start_time = System.currentTimeMillis();
        long end_time = start_time + 10000;
        while (System.currentTimeMillis() < end_time) {
            BufferedImage QR = swiftBot.getQRImage();
            QRstr = swiftBot.decodeQRImage(QR);
            if (QRstr != "") {
                break;
            }
        }
        System.out.println("Shape To Be Drawn: " + QRstr);
    }

    // Validate the shape commands in the QR code
    private static boolean validateShape(String QRstr) {
        String[] shapes = QRstr.split("&");
        if (shapes.length > 5) {
            System.out.println("Too many shapes! Maximum of 5 shapes allowed.");
            return false;
        }
        for (String shape : shapes) {
            if (!shape.startsWith("S-") && !shape.startsWith("T-") && !shape.startsWith("C-")) {
                return false;
            }
        }
        return true;
    }

    // Draws multiple shapes from the QR code input
    private static void drawMultipleShapes(String QRstr) {
        String[] shapes = QRstr.split("&");
        for (String shape : shapes) {
            drawShape1(shape);
            if (shapes.length > 1) {
                moveBackwards(movingbackdistance);
            }
        }
    }

    // Draws a single shape based on the QR code input
    private static void drawShape1(String QRstr) {
        try {
            String[] parts = QRstr.split("-");
            String shapeType = parts[0];

            switch (shapeType) {
                case "S":
                    double sideLength = Double.parseDouble(parts[1]);
                    drawSquare(sideLength);
                    break;
                case "T":
                    double side1 = Double.parseDouble(parts[1]);
                    double side2 = Double.parseDouble(parts[2]);
                    double side3 = Double.parseDouble(parts[3]);
                    drawTriangle(side1, side2, side3);
                    break;
                case "C":
                    double radius = Double.parseDouble(parts[1]);
                    drawCircle(radius);
                    break;
                default:
                    System.out.println("Invalid input, please enter a Valid Shape: " + shapeType);
                    break;
            }
        } catch (Exception e) {
            System.out.println("Error drawing shape: " + e.getMessage());
        }
    }

    // Draws a square with the given side length
    private static void drawSquare(double sideLength) {
        try {
            if (sideLength < 15 || sideLength > 85) {
                System.out.println("Invalid input! Side length must be between 15 cm and 85 cm. Please enter a Valid Shape");
                return;
            }

            int moveTime = (int) (sideLength * timemovedpercm * travelfactor);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 4; i++) {
                moveForward(moveTime);
                Thread.sleep(1000);

                turnLeft(turtimesquare);
                Thread.sleep(1000);
            }

            swiftBot.stopMove();

            long endTime = System.currentTimeMillis();
            double timeTaken = (endTime - startTime) / 1000.0;

            logSquare(sideLength, timeTaken);

            // Flashes underlights to indicate completion
            int[] green = {0, 255, 0};
            int flashamount = 3;
            int flashtime = 500;

            for (int i = 0; i < flashamount; i++) {
                swiftBot.fillUnderlights(green);
                Thread.sleep(flashtime);
                swiftBot.disableUnderlights();
                Thread.sleep(flashtime);
            }
        } catch (Exception e) {
            System.out.println("ERROR drawing square: " + e.getMessage());
        }
    }

    // Draws a circle with the given radius
     private static void drawCircle(final double radius) {
         try {
            if (radius < 15 || radius > 85) {
            	System.out.println("Invalid input! Radius must be between 15 cm and 85 cm. Please enter a Valid Shape");
            	return;
            	}
            double circumference = 2 * Math.PI * radius;
            int moveTime = (int) (circumference * timemovedpercm * circlefactor);

            long startTime = System.currentTimeMillis();


            System.out.println("Drawing circle...");


            swiftBot.move(50, 0, moveTime);

            swiftBot.stopMove();


            long endTime = System.currentTimeMillis();

            double timeTaken = (endTime - startTime) / 1000.0;


            logCircle(radius, timeTaken);


            int[] green = {0, 255, 0};

            int flashamount = 3;

            int flashtime = 500;


            for (int i = 0; i < flashamount; i++) {

            	swiftBot.fillUnderlights(green);

            	Thread.sleep(flashtime);


            	swiftBot.disableUnderlights();
            	Thread.sleep(flashtime);
            }
         } catch (Exception e) {
             System.out.println("Error drawing the circle: " + e.getMessage());
         }
     }

   
    // Draws a triangle with the given side lengths
    private static void drawTriangle(double side1, double side2, double side3) {
        try {
            if (side1 < 15 || side1 > 85 || side2 < 15 || side2 > 85 || side3 < 15 || side3 > 85) {
                System.out.println("Invalid input! Side length must be between 15 cm and 85 cm. Please enter a Valid Shape");
                return;
            }

            if (!canFormTriangle(side1, side2, side3)) {
                System.out.println("These sides cannot form a triangle.");
                return;
            }

            int moveTimeA = (int) (side1 * timemovedpercm * travelfactor);
            int moveTimeB = (int) (side2 * timemovedpercm * travelfactor);
            int moveTimeC = (int) (side3 * timemovedpercm * travelfactor * lastSideScaleFactor);

            int widerTurnTimeSecond = (int) (turnby120degrees * 1.8);
            int slightlyWiderTurnTimeLast = (int) (turnby120degrees * 1.39);

            long startTime = System.currentTimeMillis();

            System.out.println("Drawing triangle...");

            moveForward(moveTimeA);
            Thread.sleep(1000);

            turnLeft(turnby120degrees);
            Thread.sleep(1000);

            moveForward(moveTimeB);
            Thread.sleep(1000);

            turnLeft(widerTurnTimeSecond);
            Thread.sleep(1000);

            moveForward(moveTimeC);
            Thread.sleep(1000);

            turnLeft(slightlyWiderTurnTimeLast);
            Thread.sleep(1000);

            swiftBot.stopMove();

            long endTime = System.currentTimeMillis();
            double timeTaken = (endTime - startTime) / 1000.0;

            logTriangle(side1, side2, side3, timeTaken);

            // Flashes underlights to indicate completion
            int[] green = {0, 255, 0};
            int flashamount = 3;
            int flashtime = 500;

            for (int i = 0; i < flashamount; i++) {
                swiftBot.fillUnderlights(green);
                Thread.sleep(flashtime);
                swiftBot.disableUnderlights();
                Thread.sleep(flashtime);
            }
        } catch (Exception e) {
            System.out.println("Error drawing the triangle: " + e.getMessage());
        }
    }

    // Moves the robot backwards by a specific
    private static void moveBackwards(int distanceCm) {
        try {
            int moveTime = (int) (distanceCm * timemovedpercm * travelfactor);
            swiftBot.move(-50, -50, moveTime);
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Error moving backwards: " + e.getMessage());
        }
    }

    // Checks if the given sides can form a triangle
    private static boolean canFormTriangle(double side1, double side2, double side3) {
        return (side1 + side2 > side3) && (side1 + side3 > side2) && (side2 + side3 > side1);
    }

    // Moves the robot forward for a specified time
    private static void moveForward(int timeMs) throws InterruptedException {
        swiftBot.move(50, 50, timeMs);
    }

    // Turns the robot left for a specified time
    private static void turnLeft(int timeMs) throws InterruptedException {
        swiftBot.move(-50, 50, timeMs);
    }

    // Logs a square with its side length and time taken
    private static void logSquare(double sideLength, double timeSeconds) {
        double area = sideLength * sideLength;
        shapeLog += "Square: " + sideLength + " cm (Time: " + timeSeconds + " seconds)\n";
        updateLargestShape("Square", area);
        totalTime += timeSeconds;
        shapeCount++;
        squareCount++;
    }
   
    // Logs a circle with its radius and time taken
    private static void logCircle(double radius, double timeSeconds) {
        double area = Math.PI * radius * radius;
        shapeLog += "Circle: " + radius + " cm (Time: " + timeSeconds + " seconds)\n";
        updateLargestShape("Circle", area);
        totalTime += timeSeconds;
        shapeCount++;
    }

    // Logs a triangle with its side lengths and time taken
    private static void logTriangle(double side1, double side2, double side3, double timeSeconds) {
        double s = (side1 + side2 + side3) / 2.0;
        double area = Math.sqrt(s * (s - side1) * (s - side2) * (s - side3));
        shapeLog += "Triangle: " + side1 + ", " + side2 + ", " + side3 + " cm (Time: " + timeSeconds + " seconds)\n";
        updateLargestShape("Triangle", area);
        totalTime += timeSeconds;
        shapeCount++;
        triangleCount++;
    }

    // Updates the largest shape drawn
    private static void updateLargestShape(String type, double area) {
        if (area > largestArea) {
            largestArea = area;
            largestShape = type + ": " + area + " cm";
        }
    }

    // Displays the drawing log summary
    private static void displayDrawingLog() {
        System.out.println("\nDrawing Log Summary");
        System.out.println(shapeLog);
        System.out.println("Largest Shape Drawn: " + largestShape);
        System.out.println("Most Frequently Drawn Shape: " + (squareCount >= triangleCount ? "Square" : "Triangle") + " (" + Math.max(squareCount, triangleCount) + " times)");
        System.out.println("Average Drawing Time: " + (shapeCount > 0 ? totalTime / shapeCount : 0) + " seconds");
    }

    // Save the drawing log to a file
    private static void saveLog() {
        try (java.io.PrintWriter writer = new java.io.PrintWriter("shape_log.txt", "UTF-8")) {
            writer.println("Drawing Log Summary");
            writer.println(shapeLog);
            writer.println("Largest Shape : " + largestShape);
            writer.println("Most Frequent Shape: " + (squareCount >= triangleCount && squareCount >= circleCount ? "Square" : (triangleCount >= squareCount && triangleCount >= circleCount ? "Triangle" : "Circle")) + " (" + Math.max(Math.max(squareCount, triangleCount), circleCount) + " times)");
            writer.println("Average Drawing Time: " + (shapeCount > 0 ? totalTime / shapeCount : 0) + " seconds");
            System.out.println("Shape drawing log saved to shape_log.txt");
        } catch (java.io.IOException e) {
            System.out.println("Error saving log file!");
        }
    }
}
