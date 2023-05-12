import library.core.*;
import java.util.*;

public class HistoryState {
    private ArrayList<String> content;
    private PVector cursorPos;

    public HistoryState(ArrayList<String> content, PVector cursorPos) {
        this.content = new ArrayList<String>(content);
        this.cursorPos = cursorPos;
    }

    public ArrayList<String> getContent() {
        return content;
    }

    public PVector getCursorPos() {
        return cursorPos;
    }
}