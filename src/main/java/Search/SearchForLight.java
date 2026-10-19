package Search;
import swiftbot.*;
import java.awt.image.BufferedImage;
import swiftbot.Button;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.*;



public class SearchForLight {
    static SwiftBotAPI swiftBot;
    private static double[][] lightMatrix;
    private static double previousTotalBrightness = -1;

    // Logging and tracking
    private static final List<String> movementLog = new ArrayList<>();
    private static final List<String> obstacleImages = new ArrayList<>();
    private static double threshold = 0;
    private static double brightestSeen = 0;
    private static int lightDetections = 0;
    private static int obstacleCount = 0;
    private static long totalDistance = 0;
    private static long startTime;

    public static void main(String[] args) throws InterruptedException {
        try {
            swiftBot = new SwiftBotAPI();
        } catch (Exception e) {
            System.out.println("\nI2C disabled! Enable I2C and restart the bot.");
            System.exit(5);
        }

        System.out.println("Press 'A' to start the light search");
        waitForButtonPress(Button.A);

        double[][] envLightMatrix = captureLightMatrix();
        double envLeft = envLightMatrix[0][0];
        double envCenter = envLightMatrix[1][0];
        double envRight = envLightMatrix[2][0];
        threshold = (envLeft + envCenter + envRight) / 3;
        System.out.println("[INFO] Environment Threshold set to: " + threshold);

        startTime = System.currentTimeMillis();

        // Setup exit buttons early
        swiftBot.enableButton(Button.X, () -> {
            askToDisplayLog();
        });

        while (true) {
            if (detectObstacle()) {
                handleObstacle();
                continue;
            }

            lightMatrix = captureLightMatrix();
            double currentTotalBrightness = lightMatrix[0][0] + lightMatrix[1][0] + lightMatrix[2][0];

            if (previousTotalBrightness != -1 && Math.abs(currentTotalBrightness - previousTotalBrightness) < 10) {
                System.out.println("[INFO] No significant light change. Wandering randomly...");
                randomWander();
                previousTotalBrightness = currentTotalBrightness;
                continue;
            }

            previousTotalBrightness = currentTotalBrightness;
            String direction = determineDirection(lightMatrix, threshold);
            moveTowardsLight(direction);

            logAction("[LOG] Light Intensity - Left: " + lightMatrix[0][0] + ", Center: " + lightMatrix[1][0] + ", Right: " + lightMatrix[2][0]);
            logAction("[LOG] Chosen Direction: " + direction);

            Thread.sleep(1000);
        }
    }

    private static void waitForButtonPress(Button button) {
        final boolean[] buttonPressed = {false};
        swiftBot.enableButton(button, () -> {
            buttonPressed[0] = true;
            swiftBot.disableButton(button);
        });
        while (!buttonPressed[0]) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static double[][] captureLightMatrix() {
        BufferedImage image = swiftBot.takeStill(ImageSize.SQUARE_720x720);
        try {
            File outputfile = new File("captured_image.png");
            ImageIO.write(image, "png", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return processImage(image);
    }

    private static double[][] processImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] lightMatrix = new double[3][1];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = image.getRGB(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                int brightness = (int)(0.299 * red + 0.587 * green + 0.114 * blue);
                if (x < width / 3) lightMatrix[0][0] += brightness;
                else if (x < 2 * width / 3) lightMatrix[1][0] += brightness;
                else lightMatrix[2][0] += brightness;
            }
        }
        for (int i = 0; i < 3; i++) lightMatrix[i][0] /= (width / 3 * height);
        return lightMatrix;
    }

    private static String determineDirection(double[][] lightMatrix, double threshold) {
        double left = lightMatrix[0][0];
        double center = lightMatrix[1][0];
        double right = lightMatrix[2][0];
        brightestSeen = Math.max(brightestSeen, Math.max(left, Math.max(center, right)));
        lightDetections++;
        if (left > threshold && left >= center && left >= right) return "left";
        else if (right > threshold && right >= center && right >= left) return "right";
        else if (center > threshold) return "forward";
        else return new Random().nextBoolean() ? "left" : "right";
    }

    private static void moveTowardsLight(String direction) {
        swiftBot.fillUnderlights(new int[]{0, 255, 0});
        switch (direction) {
            case "left": swiftBot.move(-30, 30, 500); totalDistance += 10; movementLog.add("Left 10cm"); break;
            case "right": swiftBot.move(30, -30, 500); totalDistance += 10; movementLog.add("Right 10cm"); break;
            case "forward": swiftBot.move(50, 50, 1000); totalDistance += 20; movementLog.add("Forward 20cm"); break;
        }
    }

    private static boolean detectObstacle() {
        return swiftBot.useUltrasound() < 50;
    }

    private static void handleObstacle() throws InterruptedException {
        obstacleCount++;
        BufferedImage obstacleImage = swiftBot.takeStill(ImageSize.SQUARE_720x720);
        String filename = "obstacle_" + System.currentTimeMillis() + ".png";
        try {
            ImageIO.write(obstacleImage, "png", new File(filename));
            obstacleImages.add(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        swiftBot.fillUnderlights(new int[]{255, 0, 0});
        for (int i = 10; i > 0; i--) {
            System.out.println("Waiting... " + i);
            Thread.sleep(1000);
        }
        if (!detectObstacle()) {
            swiftBot.fillUnderlights(new int[]{0, 255, 0});
            Thread.sleep(1000);
            swiftBot.fillUnderlights(new int[]{0, 0, 0});
            return;
        }
        lightMatrix = captureLightMatrix();
        String newDirection = secondBrightestDirection(lightMatrix[0][0], lightMatrix[1][0], lightMatrix[2][0]);
        moveTowardsLight(newDirection);
    }

    private static String secondBrightestDirection(double left, double center, double right) {
        Map<String, Double> map = new HashMap<>();
        map.put("left", left); map.put("center", center); map.put("right", right);
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(map.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sorted.get(1).getKey();
    }

    private static void randomWander() {
        Random rand = new Random();
        String direction = rand.nextBoolean() ? "left" : "right";
        System.out.println("Wandering " + direction);
        if (direction.equals("left")) swiftBot.move(-30, 30, 1000);
        else swiftBot.move(30, -30, 1000);
        totalDistance += 10;
        movementLog.add("Wandered " + direction + " 10cm");
    }

    private static void logAction(String message) {
        try {
            FileWriter writer = new FileWriter("swiftbot_log.txt", true);
            writer.write(message + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void askToDisplayLog() {
        System.out.println("Press 'Y' to display log or 'X' to skip.");
        swiftBot.enableButton(Button.Y, () -> {
            displayLog();
            System.exit(0);
        });
        swiftBot.enableButton(Button.X, () -> {
            writeLogToFile();
            System.exit(0);
        });
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void displayLog() {
        System.out.println("\n------ SwiftBot Journey Log ------");
        System.out.println("Threshold: " + threshold);
        System.out.println("Brightest intensity seen: " + brightestSeen);
        System.out.println("Light detections: " + lightDetections);
        System.out.println("Total distance: " + totalDistance + " cm");
        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) / 1000 + "s");
        System.out.println("Movements:");
        for (String move : movementLog) System.out.println("- " + move);
        System.out.println("Obstacles: " + obstacleCount);
        for (String path : obstacleImages) System.out.println("Saved: " + path);
    }

    private static void writeLogToFile() {
        try {
            FileWriter writer = new FileWriter("swiftbot_summary_log.txt");
            writer.write("Threshold: " + threshold + "\n");
            writer.write("Brightest intensity seen: " + brightestSeen + "\n");
            writer.write("Light detections: " + lightDetections + "\n");
            writer.write("Total distance: " + totalDistance + " cm\n");
            writer.write("Execution time: " + (System.currentTimeMillis() - startTime) / 1000 + "s\n");
            writer.write("Movements:\n");
            for (String move : movementLog) writer.write("- " + move + "\n");
            writer.write("Obstacles: " + obstacleCount + "\n");
            for (String path : obstacleImages) writer.write("Saved: " + path + "\n");
            writer.close();
            System.out.println("Log saved to swiftbot_summary_log.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
