import java.io.File;
import java.util.*;

import library.core.*;

class Sketch extends Applet {

    private TitleScreen title;
    private Editor editor;

    private int state = 0;

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

        title = new TitleScreen(true);
    }

    public void draw() {
        if (state == 0) {
            // Title screen
            title.draw();

            if (editor != null) {
                delete(editor);
                editor = null;
            }

            if (title.getState().length() != 0) {
                String titleState = title.getState();
                switch (titleState) {
                    case "new":
                        createEditor();
                        break;
                    case "new insert":
                        createEditor();
                        editor.mimicKeyPress('i');
                        break;
                    case "open":
                        editor = new Editor(this);
                        if (!editor.openExplorer()) {
                            createTitle();
                            return;
                        }
                        break;
                    default:
                        File file = new File(titleState);
                        createEditor();
                        editor.setFile(file);
                        break;
                }
                state = 1;
            }
        } else if (state == 1) {
            editor.draw();
        }
    }

    public void setState(int state) {
        this.state = state;
        if (state == 0) {
            createTitle();
        } else if (state == 1) {
            createEditor();
        }
    }

    public void createEditor() {
        editor = new Editor(this);
        delete(title);
        title = null;
    }

    public void createTitle() {
        title = new TitleScreen(false);
        delete(editor);
        editor = null;
    }

    public void onExit() {
        LinkedHashMap<String, String> properties = loadProperties("settings.properties");
        properties.put("frameWidth", Integer.toString(width));
        properties.put("frameHeight", Integer.toString(height));
        saveProperties(properties, "settings.properties");
    }
}