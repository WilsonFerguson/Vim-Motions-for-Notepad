import java.io.File;
import java.util.*;

import library.core.*;

class Sketch extends Applet {

    TitleScreen title;
    Editor editor;

    public void setup() {
        LinkedHashMap<String, String> properties = loadProperties("settings.properties");
        int frameWidth = parseInt(properties.get("frameWidth"));
        if (frameWidth == 0) {
            PVector screenSize = getScaledScreenSize();
            size((int) screenSize.x / 2, (int) screenSize.y / 2);
        } else {
            int frameHeight = parseInt(properties.get("frameHeight"));
            size(frameWidth, frameHeight);
        }
        setResizable(true);

        setTitle("Vim Motions for Notepad");
        exitOnEscape(false);

        title = new TitleScreen();
    }

    public void draw() {
        if (title != null) {
            title.draw();
            if (title.getState().length() != 0) {
                String state = title.getState();
                switch (state) {
                    case "new":
                        editor = new Editor();
                        break;
                    case "new insert":
                        editor = new Editor();
                        editor.mimicKeyPress('i');
                        break;
                    case "open":
                        editor = new Editor();
                        if (!editor.openExplorer()) {
                            editor = null;
                            title = new TitleScreen();
                            return;
                        }
                        break;
                    default:
                        File file = new File(state);
                        editor = new Editor();
                        editor.setFile(file);
                        break;
                }
                title = null;
            }
        }
        if (editor != null)
            editor.draw();
    }

    public void onExit() {
        LinkedHashMap<String, String> properties = loadProperties("settings.properties");
        properties.put("frameWidth", Integer.toString(width));
        properties.put("frameHeight", Integer.toString(height));
        saveProperties(properties, "settings.properties");
    }
}