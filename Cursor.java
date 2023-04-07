import java.util.*;
import library.core.*;

public class Cursor extends PComponent implements EventIgnorer {
    private enum CharType {
        LETTER, PUNCTUATION, SPACE, NUMBER
    }

    int x;
    int y;

    private List<String> content;

    private boolean visible = true;

    public Cursor(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Cursor() {
        this(0, 0);
    }

    public void toggleVisibility() {
        visible = !visible;
    }

    public void makeVisible() {
        visible = true;
    }

    public void makeInvisible() {
        visible = false;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public PVector getPosition() {
        return new PVector(x, y);
    }

    public void left() {
        if (x == 0 && y == 0)
            return;

        if (x == 0) {
            y--;
            x = getEndOfLine();
            return;
        }

        x--;
    }

    public void right() {
        if (isEndOfLine() && y == content.size() - 1)
            return;

        if (isEndOfLine()) {
            y++;
            x = 0;
            return;
        }

        x++;
    }

    public void up() {
        if (y == 0)
            return;

        y--;
        if (x > getEndOfLine())
            x = getEndOfLine();
    }

    public void down() {
        if (y == content.size() - 1)
            return;

        y++;
        if (x > getEndOfLine())
            x = getEndOfLine();
    }

    public void findLastNonWhitespace() {
        x = findLastNonWhitespace(content.get(y));
    }

    public void findFirstNonWhitespace() {
        x = findFirstNonWhitespace(content.get(y));
    }

    public void nextWord() {
        if (isEndOfLine() && y == content.size() - 1)
            return;

        if (isEndOfLine()) {
            y++;
            findFirstNonWhitespace();
            return;
        }

        // Keep going until I reach a new char type
        CharType type = getCharType(x, y);
        while (isInLine() && getCharType(x, y) == type)
            x++;

        // If I'm on a space, move forward until I reach a non-space
        if (getCharType(x, y) == CharType.SPACE)
            x += findFirstNonWhitespace(content.get(y).substring(x));
    }

    public void previousWord() {
        if (x == 0 && y == 0)
            return;

        if (x == 0 && y > 0) {
            y--;
            findLastNonWhitespace();
            return;
        }

        boolean middleOfWord = false;
        if (getCharType(x - 1, y) == getCharType(x, y))
            middleOfWord = true;

        if (middleOfWord) {
            // Keep going to the left until reached a different char type
            CharType type = getCharType(x, y);
            while (x > 0 && getCharType(x, y) == type)
                x--;

            if (x != 0)
                x++; // Move back one char so that I'm on the first char of the word
            return;
        }

        // Keep going to the left until I reach a new type
        CharType type = getCharType(x, y);
        while (x > 0 && getCharType(x, y) == type)
            x--;

        type = getCharType(x, y);
        if (type == CharType.SPACE) {
            // Keep going to the left until I reach a new type
            while (x > 0 && getCharType(x, y) == type)
                x--;
        }

        // Go to the start of my new word
        type = getCharType(x, y);
        while (x > 0 && getCharType(x, y) == type)
            x--;

        if (x > 0)
            x += 1;
    }

    public void nextWordWithPunctuation() {
        if (isEndOfLine() && y == content.size() - 1)
            return;

        if (isEndOfLine()) {
            y++;
            findFirstNonWhitespace();
            return;
        }

        if (content.get(y).charAt(x) == ' ') {
            x += findFirstNonWhitespace(content.get(y).substring(x));
            return;
        }

        int spaceIndex = content.get(y).indexOf(' ', x);
        if (spaceIndex == -1)
            x = content.get(y).length();
        else
            x = spaceIndex + 1;
    }

    public void previousWordWithPunctuation() {
        if (x == 0 && y == 0)
            return;

        if (x == 0 && y > 0) {
            y--;
            findLastNonWhitespace();
            return;
        }

        // Default substring is from 0 to x (which works good if I'm in the middle of a
        // word)
        String substring = content.get(y).substring(0, x);

        // If I'm a space or right next to a space, I need to substring to x - 1
        if (content.get(y).charAt(x) == ' ' || content.get(y).charAt(x - 1) == ' ')
            substring = content.get(y).substring(0, x - 1);

        int spaceIndex = substring.lastIndexOf(' ');
        if (spaceIndex == -1)
            x = 0;
        else
            x = spaceIndex + 1;
    }

    public void endOfWord() {
        if (isEndOfLine() && y == content.size() - 1)
            return;

        if (isEndOfLine()) {
            y++;
            findFirstNonWhitespace();
            return;
        }

        // Keep going until I reach a new char type
        CharType type = getCharType(x, y);
        while (isInLine() && getCharType(x, y) == type)
            x++;

        // If I'm on a space, move forward until I reach a non-space
        if (getCharType(x, y) == CharType.SPACE)
            x += findFirstNonWhitespace(content.get(y).substring(x));
    }

    public void deleteCurrentCharacter() {
        if (!onCharacter())
            return;

        String line = content.get(y);
        content.set(y, line.substring(0, x) + line.substring(x + 1));
    }

    public void deleteToLineEnd() {
        if (isEndOfLine())
            return;

        String line = content.get(y);
        content.set(y, line.substring(0, x));
    }

    public void newLineBelow() {
        content.add(y + 1, "");
        y++;
        x = 0;
    }

    public void newLineAbove() {
        content.add(y, "");
        x = 0;
    }

    private int findLastNonWhitespace(String line) {
        for (int i = line.length() - 1; i >= 0; i--)
            if (!Character.isWhitespace(line.charAt(i)))
                return i + 1;
        return 0;
    }

    private int findFirstNonWhitespace(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i)))
                return i;
        }
        return 0;
    }

    private CharType getCharType(int x, int y) {
        char c = content.get(y).charAt(x);

        if (Character.isLetter(c))
            return CharType.LETTER;
        if (Character.isDigit(c))
            return CharType.NUMBER;
        if (c == ' ')
            return CharType.SPACE;

        return CharType.PUNCTUATION;
    }

    private boolean isEndOfLine() {
        return x == content.get(y).length() - 1;
    }

    private boolean isInLine() {
        return !isEndOfLine();
    }

    public int getEndOfLine() {
        return max(content.get(y).length() - 1, 0);
    }

    private boolean onCharacter() {
        try {
            content.get(y).charAt(x);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private PVector getTextPosition(int x, int y) {
        if (content.get(y).length() == 0)
            return new PVector(0, textHeight("A") * y);

        return new PVector(textWidth(content.get(y).substring(0, x)), textHeight("A") * y);
    }

    private void clamp(Mode mode) {
        int xLimit = mode == Mode.INSERT ? content.get(y).length() : content.get(y).length() - 1;
        x = min(x, xLimit);
        y = min(y, content.size() - 1);
        x = max(x, 0);
        y = max(y, 0);
    }

    public PVector getPos() {
        return getTextPosition(x, y);
    }

    public void draw(Mode mode) {
        clamp(mode);

        if (!visible)
            return;

        PVector pos = getPos();
        float lineHeight = textHeight("A");
        if (mode == Mode.INSERT) {
            rect(pos.x, pos.y, 2, lineHeight);
        } else {
            if (content.get(y).length() == 0) {
                rect(pos.x, pos.y, textWidth("A"), lineHeight);
                return;
            }

            char c = content.get(parseInt(y)).charAt(parseInt(x));
            rect(pos.x, pos.y, textWidth(c + ""), lineHeight);
        }
    }
}
