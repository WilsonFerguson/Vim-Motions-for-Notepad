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
        LinkedHashMap<String, String> properties = loadProperties("settings.properties");
        backgroundColor = color(properties.get("backgroundColor"));
        textColor = color(properties.get("textColor"));
        textFont(properties.get("fontFamily"));

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

        // drawCommand("New file", 't', width / 4, height / 2 + height / 4);
        // drawCommand("Open file", 'e', width / 2, height / 2 + height / 4);
        // drawCommand("Delete file", 'd', width * 3 / 4, height / 2 + height / 4);
        float y = height / 2 + height / 4;
        HashMap<String, Character> commands = new HashMap<>();
        commands.put("New file", 't');
        commands.put("Open file", 'e');
        commands.put("Delete file", 'd');
        commands.put("Remove file", 'r');

        int i = 0;
        float margin = width / 6;
        for (String command : commands.keySet()) {
            float x = map(i, 0, commands.size() - 1, margin, width - margin);
            drawCommand(command, commands.get(command), x, y);
            i++;
        }

        // Draw the motion
        textAlign(LEFT);
        fill(textColor);
        text(motion, 0, height - textHeight(motion));
    }

    public void keyPressed() {
        // Make sure the title screen is active
        if (state.length() > 0)
            return;

        if (keyString.equals("Backspace")) {
            if (motion.length() > 0)
                motion = motion.substring(0, motion.length() - 1);
            return;
        }
        if (keyString.equals("Escape")) {
            motion = "";
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

        String newMotion = motion.substring(1);

        switch (newMotion) {
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

    private boolean handleDeletion() {
        if (!motion.startsWith("d") || motion.length() < 2)
            return false;

        motion = motion.substring(1);
        int index = parseInt(motion) - 1;
        if (index < 0 || index >= recentFiles.length)
            return false;

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
        return true;
    }

    private boolean handleRemove() {
        if (!motion.startsWith("r") || motion.length() < 2)
            return false;

        motion = motion.substring(1);
        int index = parseInt(motion) - 1;
        if (index < 0 || index >= recentFiles.length)
            return false;

        // Remove the file from the list
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < recentFiles.length; i++) {
            if (i != index)
                files.add(recentFiles[i]);
        }

        recentFiles = files.toArray(new String[files.size()]);
        saveStrings(recentFiles, "RecentFiles.txt");

        motion = "";
        return true;
    }

    private void calculateMotion() {
        switch (motion) {
            case "t":
                state = "new";
                break;
            case "n":
                state = "new";
                break;
            case "i":
                state = "new insert";
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
                if (handleDeletion())
                    break;
                if (handleRemove())
                    break;

                break;
        }
    }

}
