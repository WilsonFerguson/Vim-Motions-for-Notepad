import library.core.*;

// TODO add a feature that saves the past 10 opened files to a txt and then have an option to open a recent file
class Sketch extends Applet {

    Editor editor;

    public void setup() {
        // Set width and height to be half of the screen size
        PVector screenSize = getScaledScreenSize();
        size((int) screenSize.x / 2, (int) screenSize.y / 2);
        setResizable(true);

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