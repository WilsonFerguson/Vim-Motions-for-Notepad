import java.io.File;
import java.util.*;

import library.core.*;
import lorem.*;

public class TitleScreen extends PComponent {

    private color backgroundColor, textColor;
    private String state = "";

    private String[] recentFiles;

    // Keyboard inputs
    private String motion = "";

    private boolean startUpScreen = true;
    private String[] message;
    private int messageIndex = 0;
    private int filesDownloaded = 0;
    private double internetSpeed = 70; // 0-100

    public TitleScreen(boolean startUpScreen) {
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

        this.startUpScreen = startUpScreen;
        if (startUpScreen) {
            if (!bool(properties.get("firstTimeScreen")))
                this.startUpScreen = false;
        }
        if (startUpScreen) {
            Lorem lorem = LoremIpsum.getInstance();

            message = new String[100];
            for (int i = 0; i < 100; i++) {
                String line = "Downloading " + i + "% ";
                line += lorem.getUrl();
                message[i] = line;
            }
        }
    }

    public String getState() {
        return state;
    }

    public boolean isStartUpScreen() {
        return startUpScreen;
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

        boolean hovered = mouseX > x - w / 2 && mouseX < x + w / 2 && mouseY > y - h / 2 && mouseY < y + h / 2;
        if (mousePressed && hovered) {
            state = recentFiles[index];
        }

        // Draw the outline
        stroke(textColor);
        strokeWeight(1.5);
        noFill();
        if (hovered)
            fill(0, 50);
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

    private void drawTitleScreen() {
        background(backgroundColor);
        textSize(0.065 * width / 4 / getUIScale());

        fill(textColor);
        rectMode(CENTER);
        for (int i = 0; i < recentFiles.length; i++) {
            drawRecentFile(recentFiles[i], i);
        }

        float y = height / 2 + height / 4;
        LinkedHashMap<String, Character> commands = new LinkedHashMap<>();
        commands.put("New file", 'T');
        commands.put("Open file", 'E');
        commands.put("Delete file", 'D');
        commands.put("Remove file", 'R');

        int i = 0;
        float margin = width / 6;
        for (String command : commands.keySet()) {
            float x = map(i, 0, commands.size() - 1, margin, width - margin);
            drawCommand(command, commands.get(command), x, y);
            i++;
        }
    }

    private void drawMessage() {
        push();
        float lineHeight = textAscent() + textDescent();
        translate(0, lineHeight / 2); // Move text down so that the top is at y = 0
        // If the message is too long, translate everything up
        float bottom = height * 2 / 3;
        if (lineHeight * messageIndex > bottom) {
            translate(0, -lineHeight * (messageIndex - bottom / lineHeight));
        }

        for (int i = 0; i < message.length; i++) {
            if (i > messageIndex)
                break;
            text(message[i], 0, 0);
            translate(0, lineHeight);
        }
        if (random(1) < map(internetSpeed, 0, 100, 0, 0.9)) {
            messageIndex++;
        }
        pop();
    }

    private void drawStartUpScreen() {
        background(backgroundColor);
        LinkedHashMap<String, String> properties = loadProperties("settings.properties");
        textSize(parseInt(properties.get("fontSize")) * 0.75);
        fill(textColor);
        noStroke();

        if (messageIndex < message.length)
            drawMessage();
        else {
            float lineHeight = textAscent() + textDescent();
            translate(0, lineHeight / 2);

            text("Connection to the US government complete.", 0, 0);
            translate(0, lineHeight);
            text("Please wait, some files failed to download and need to be retried.", 0, 0);

            int filesToDownload = 10;
            for (int i = 0; i < filesDownloaded; i++) {
                translate(0, lineHeight);
                int fileNumber = i + 1;
                String text = "Finished downloading file " + fileNumber + " of " + filesToDownload;
                if (fileNumber > filesToDownload)
                    text += " :)";
                text(text, 0, 0);
            }
            if (filesDownloaded <= filesToDownload) {
                if (random(1) < map(internetSpeed, 0, 100, 0, 0.05))
                    filesDownloaded++;
            } else {
                translate(0, lineHeight);
                text("Finished downloading the remaining viruses.", 0, 0);
                translate(0, lineHeight * 2);
                text("You are good to go! To get started, press \"enter\" to go to the title screen.", 0, 0);
                translate(0, lineHeight);
                text("If you are ever stuck, type \":help\" or \"?\" to pull up the doc", 0, 0);

            }
        }
    }

    public void draw() {
        if (startUpScreen)
            drawStartUpScreen();
        else
            drawTitleScreen();

        // Draw the motion
        textSize(0.065 * width / 4 / getUIScale()); // Divide by UI scale to make sure text isn't scaled up on high DPI
                                                    // screens
        textAlign(LEFT);
        fill(textColor);
        text(motion, 0, height - textHeight(motion));
    }

    public void keyPressed() {
        // Make sure the title screen is active
        if (state.length() > 0)
            return;

        if (startUpScreen) {
            if (keyString.equals("Enter")) {
                startUpScreen = false;
                LinkedHashMap<String, String> properties = loadProperties("settings.properties");
                properties.put("firstTimeScreen", "false");
                saveProperties(properties, "settings.properties");
            }
        }

        if (keyString.equals("Backspace")) {
            if (motion.length() > 0)
                motion = motion.substring(0, motion.length() - 1);
            return;
        }
        if (keyString.equals("Escape")) {
            motion = "";
            return;
        }
        if (keyString.equals("Enter")) {
            if (handleCommand()) {
                motion = "";
                return;
            }
        }

        String[] allowedKeys = { "Semicolon", "Slash" };
        if (keyString.length() > 1 && !Arrays.asList(allowedKeys).contains(keyString))
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
            case "wq":
            case "q!":
            case "qa!":
            case "qa":
            case "wqa":
                exit();
                return true;
            case "help":
                openInBrowser(
                        "https://docs.google.com/document/d/1fQ1oPZZzbYpaEFzKpX9-POfc1pGENRN_j8IovHwYWak/edit?usp=sharing");
                return true;
        }

        return false;
    }

    private boolean handleDeletion() {
        if (!motion.startsWith("d") || motion.length() < 2)
            return false;

        String newMotion = motion.substring(1);
        if (!Helper.isInt(newMotion))
            return false;
        int index = parseInt(newMotion) - 1;
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
            case "?":
                openInBrowser(
                        "https://docs.google.com/document/d/1fQ1oPZZzbYpaEFzKpX9-POfc1pGENRN_j8IovHwYWak/edit?usp=sharing");
                motion = "";
                break;
            default:
                if (handleNumber())
                    break;
                if (handleDeletion())
                    break;
                if (handleRemove())
                    break;

                break;
        }
    }

}
