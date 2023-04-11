import java.io.File;
import java.util.*;

import library.core.*;

public class TitleScreen extends PComponent {

    private color backgroundColor, textColor;
    private String state = "";

    private String[] recentFiles;

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

        // float x = (index % 3 + 1) * (width / 4);
        // float y = (index / 3 + 1) * (height / 4);
        float xLeft = width / 8 + w / 4;
        float xRight = width - xLeft;
        float yTop = height / 8 + h / 4;
        float yBottom = height / 2 + h / 4;
        float x = map(index % 3, 0, 2, xLeft, xRight);
        float y = map(index / 3, 0, 2, yTop, yBottom);

        stroke(textColor);
        strokeWeight(1.5);
        noFill();
        rect(x, y, w, h, 10);

        noStroke();
        fill(150);
        textAlign(LEFT);
        float numberX = x - w / 2 + textWidth("1") / 2.5f;
        float numberY = y - h / 2 + textHeight("1") / 2;
        text(index + 1, numberX, numberY);

        fill(textColor);
        textAlign(CENTER);

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

        noFill();
        stroke(textColor);
        strokeWeight(1.5);
        float commandX = x - totalWidth / 2 + commandWidth / 2;
        rect(commandX, y, commandWidth, commandWidth, 10);

        fill(textColor);
        textAlign(CENTER);
        noStroke();
        text(command, commandX, y);

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
        switch (key) {
            case 't':
                state = "new";
                break;
            case 'e':
                state = "open";
                break;
            default:
                if (key >= '1' && key <= '9') {
                    int index = key - '1';
                    if (index < recentFiles.length) {
                        state = recentFiles[index];
                    }
                }
                break;
        }
    }

}
