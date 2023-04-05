import library.core.*;
import java.awt.*;

// TODO add a feature that saves the past 10 opened files to a txt and then have an option to open a recent file
class Sketch extends Applet {

    Editor editor;

    public void setup() {
        size(1280, 720);
        println(displayWidth, displayHeight, width, height);

        setTitle("Vim Motions for Notepad");
        exitOnEscape(false);

        createEditor();
    }

    public void createEditor() {
        editor = new Editor();
    }

    public void draw() {
        editor.draw();
    }
}