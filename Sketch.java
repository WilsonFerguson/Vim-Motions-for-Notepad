import library.core.*;

// TODO add a feature that saves the past 10 opened files to a txt and then have an option to open a recent file
class Sketch extends Applet {

    Editor editor;

    public void setup() {
        // int idealWidth = 1280;
        // int idealHeight = 720;
        // if (getScaledScreenSize().x < 1920) {
        // double widthPercent = getScaledScreenSize().x / 1920;
        // double heightPercent = getScaledScreenSize().y / 1080;
        // idealWidth = (int) (idealWidth * widthPercent);
        // idealHeight = (int) (idealHeight * heightPercent);
        // }
        // size(idealWidth, idealHeight);

        // Set width and height to be half of the screen size
        PVector screenSize = getScaledScreenSize();
        size((int) screenSize.x / 2, (int) screenSize.y / 2);

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