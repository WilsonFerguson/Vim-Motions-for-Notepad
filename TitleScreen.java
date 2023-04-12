import java.io.File;
import java.util.*;

import library.core.*;

public class TitleScreen extends PComponent {

    private color backgroundColor, textColor;
    private String state = "";

    private String[] recentFiles;

    // Keyboard inputs
    private String motion = "";

    public TitleScreen() {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("settings.properties"));
            backgroundColor = color(properties.getProperty("backgroundColor"));
            textColor = color(properties.getProperty("textColor"));
            textFont(properties.getProperty("fontFamily"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        recentFiles = loadStrings("RecentFiles.txt");
        // Remove any files that don't exist
        List<String> files = new ArrayList<String>();
        for (int i = 0; i < recentFiles.length; i++) {
            if (new File(recentFiles[i]).exists())
                files.add(recentFiles[i]);
        }

        recentFiles = files.toArray(new String[files.size()]);
        saveStrings(recentFiles, "RecentFiles.txt");
    }

    public String getState() {
        return state;
    }

    private void drawRecentFile(String file, int index) {
        float w = width / 4;
        float h = w / 3;

        // Calculate the position of the button thing
        float xLeft = width / 8 + w / 4;
        float xRight = width - xLeft;
        float yTop = height / 8 + h / 4;
        float yBottom = height / 2 + h / 4;
        float x = map(index % 3, 0, 2, xLeft, xRight);
        float y = map(index / 3, 0, 2, yTop, yBottom);

        // Draw the outline
        stroke(textColor);
        strokeWeight(1.5);
        noFill();
        rect(x, y, w, h, 10);

        // Draw the number
        noStroke();
        fill(150);
        textAlign(LEFT);
        float numberX = x - w / 2 + textWidth("1") / 2.5f;
        float numberY = y - h / 2 + textHeight("1") / 2;
        text(index + 1, numberX, numberY);

        // Draw the filename
        fill(textColor);
        textAlign(CENTER);

        // Only show the name of the file, not the path and shorten it if it's too long
        String filename = file.substring(file.lastIndexOf(File.separator) + 1);
        filename = filename.substring(0, filename.lastIndexOf("."));
        float maxChars = w / textWidth("a");
        if (filename.length() > maxChars) {
            filename = filename.substring(0, parseInt(maxChars));
        }

        text(filename, x, y);
    }

    private void drawCommand(String text, char command, float x, float y) {
        float margin = 10; // Margin between text and command
        float commandWidth = textWidth(command + "") * 2.5f;
        float totalWidth = textWidth(text) + margin + commandWidth;

        // Draw outline
        noFill();
        stroke(textColor);
        strokeWeight(1.5);
        float commandX = x - totalWidth / 2 + commandWidth / 2;
        rect(commandX, y, commandWidth, commandWidth, 10);

        // Draw command letter
        fill(textColor);
        textAlign(CENTER);
        noStroke();
        text(command, commandX, y);

        // Draw command description
        textAlign(LEFT);
        text(text, commandX + commandWidth / 2 + margin, y);
    }

    public void draw() {
        background(backgroundColor);
        textSize(0.065 * width / 4);

        fill(textColor);
        rectMode(CENTER);
        for (int i = 0; i < recentFiles.length; i++) {
            drawRecentFile(recentFiles[i], i);
        }

        drawCommand("New file", 't', width / 3, height / 2 + height / 4);
        drawCommand("Open file", 'e', width * 2 / 3, height / 2 + height / 4);
    }

    public void keyPressed() {
        if (keyString.equals("Backspace")) {
            if (motion.length() > 0)
                motion = motion.substring(0, motion.length() - 1);
            return;
        }
        if (keyString.length() > 1 && !keyString.equals("Semicolon"))
            return;

        motion += Character.toLowerCase(key);
        calculateMotion();
    }

    private boolean handleNumber() {
        if (motion.length() != 1)
            return false;

        char key = motion.charAt(0);
        if (key < '1' || key > '9')
            return false;

        int index = key - '1';
        if (index < recentFiles.length) {
            state = recentFiles[index];
        }
        motion = "";
        return true;
    }

    private boolean handleCommand() {
        if (!motion.startsWith(":") || motion.length() < 2)
            return false;

        motion = motion.substring(1);

        switch (motion) {
            case "q":
                exit();
                return true;
            case "wq":
                exit();
                return true;
            case "q!":
                exit();
                return true;
            case "qa":
                exit();
                return true;
        }

        return false;
    }

    private void handleDeletion() {
        if (!motion.startsWith("d") || motion.length() < 2)
            return;

        motion = motion.substring(1);
        int index = parseInt(motion) - 1;
        if (index < 0 || index >= recentFiles.length)
            return;

        // Delete the file
        File file = new File(recentFiles[index]);
        file.delete();

        // Remove the file from the list
        List<String> files = new ArrayList<String>();
        for (int i = 0; i < recentFiles.length; i++) {
            if (i != index)
                files.add(recentFiles[i]);
        }

        recentFiles = files.toArray(new String[files.size()]);
        saveStrings(recentFiles, "RecentFiles.txt");

        motion = "";
    }

    private void calculateMotion() {
        switch (motion) {
            case "t":
                state = "new";
                break;
            case "n":
                state = "new";
                break;
            case "e":
                state = "open";
                break;
            case "o":
                state = "open";
                break;
            default:
                if (handleNumber())
                    break;
                if (handleCommand())
                    break;
                handleDeletion();

        }
    }

}
