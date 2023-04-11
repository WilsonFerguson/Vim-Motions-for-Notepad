import java.io.File;

import library.core.*;

// TODO add a feature that saves the past 10 opened files to a txt and then have an option to open a recent file
class Sketch extends Applet {

    TitleScreen title;
    Editor editor;

    public void setup() {
        // Set width and height to be half of the screen size
        PVector screenSize = getScaledScreenSize();
        size((int) screenSize.x / 2, (int) screenSize.y / 2);
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
                if (state.equals("new")) {
                    editor = new Editor();
                } else if (state.equals("open")) {
                    editor = new Editor();
                    editor.openExplorer();
                } else {
                    File file = new File(state);
                    editor = new Editor();
                    editor.setFile(file);
                }
                title = null;
            }
        }
        if (editor != null)
            editor.draw();
    }
}