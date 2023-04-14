import library.core.Applet;

public class Main {
   public static void main(String[] args) {
      Applet.fixWindowsScaling();
      new Sketch().startApplet();
   }
}