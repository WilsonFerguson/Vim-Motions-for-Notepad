import java.util.*;
import library.core.*;

public class Cursor extends PComponent implements EventIgnorer {
    private enum CharType {
        LETTER, PUNCTUATION, SPACE, NUMBER
    }

    private enum BracketType {
        CURLY, SQUARE, PARENTHESIS, TAG, NONE
    }

    int x;
    int y;

    private Editor editor;
    private List<String> content;

    private boolean visible = true;

    public Cursor(Editor editor, int x, int y) {
        this.x = x;
        this.y = y;
        this.editor = editor;
        content = editor.getContent();
    }

    public Cursor(Editor editor) {
        this(editor, 0, 0);
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

    // public void setContent(List<String> content) {
    // this.content = content;
    // }

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

        // If the char to my right is not my type, I'm at the end of a word and should
        // first skip to it
        if (getCharType(x, y) != getCharType(x + 1, y))
            nextWord();

        // If I'm at the end of the line after moving, I'm done
        if (isEndOfLine())
            return;

        // Keep going until I reach a new char type
        CharType type = getCharType(x, y);
        while (isInLine() && getCharType(x, y) == type)
            x++;

        // Go back one
        x--;
    }

    public void endOfWordWithPunctuation() {
        if (isEndOfLine() && y == content.size() - 1)
            return;

        if (isEndOfLine()) {
            y++;
            findFirstNonWhitespace();
            return;
        }

        // If the char to my right is a space, I'm at the end of a word and should skip
        // to the next one
        if (getCharType(x + 1, y) == CharType.SPACE) {
            nextWordWithPunctuation();
        }

        // If I'm at the end of the line after moving, I'm done
        if (isEndOfLine())
            return;

        // Keep going until I reach a space
        while (isInLine() && getCharType(x, y) != CharType.SPACE)
            x++;

        // If I'm at the end of the line, I'm done
        if (isEndOfLine())
            return;

        // Go back one
        x--;
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

    public void findMatchingBracket() {
        BracketType bracketType = getBracketType(x, y);
        char bracketChar = content.get(y).charAt(x);
        int bracketCount = isOpeningBracket(bracketChar) ? 1 : -1;

        int initialX = x;
        int initialY = y;
        boolean firstRound = true;
        while (bracketCount != 0) {
            BracketType currentBracketType = getBracketType(x, y);
            if (currentBracketType == bracketType) {
                if (firstRound)
                    firstRound = false;
                else {
                    if (isOpeningBracket(content.get(y).charAt(x)))
                        bracketCount++;
                    else
                        bracketCount--;
                }
            }

            if (bracketCount == 0)
                return;

            int previousX = x;
            int previousY = y;
            if (isOpeningBracket(bracketChar))
                right();
            else
                left();

            if (x == previousX && y == previousY) {
                x = initialX;
                y = initialY;
                return;
            }

        }
    }

    public void pasteAfter() {
        String data = getClipboardContents();
        String[] lines = data.split("\n");
        for (int i = 0; i < lines.length; i++) {
            content.add(y + i + 1, lines[i]);
        }
        y += lines.length;
        x = getEndOfLine();
    }

    public void pasteBefore() {
        String data = getClipboardContents();
        String[] lines = data.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int index = y - (lines.length - i);
            index = max(0, index);
            content.add(index, lines[i]);
        }
        y += lines.length - 1;
        x = getEndOfLine();
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

    private BracketType getBracketType(int x, int y) {
        char c = content.get(y).charAt(x);

        if (c == '(' || c == ')')
            return BracketType.PARENTHESIS;
        if (c == '[' || c == ']')
            return BracketType.SQUARE;
        if (c == '{' || c == '}')
            return BracketType.CURLY;
        if (c == '<' || c == '>')
            return BracketType.TAG;

        return BracketType.NONE;
    }

    private boolean isOpeningBracket(char c) {
        return c == '(' || c == '[' || c == '{' || c == '<';
    }

    private boolean isEndOfLine() {
        if (content.get(y).length() == 0)
            return true;
        return x == getEndOfLine();
    }

    private boolean isInLine() {
        return !isEndOfLine();
    }

    public int getEndOfLine() {
        // return max(content.get(y).length() - 1, 0);
        if (editor.getMode() == Mode.INSERT)
            return content.get(y).length();

        return max(content.get(y).length() - 1, 0);
    }

    public boolean isEndOfContent() {
        return y == content.size() - 1 && isEndOfLine();
    }

    public boolean isStartOfContent() {
        return y == 0 && x == 0;
    }

    private boolean onCharacter() {
        try {
            content.get(y).charAt(x);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean toRightOf(Cursor other) {
        if (y > other.y)
            return true;

        if (y < other.y)
            return false;

        return x > other.x;
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
        content = editor.getContent();
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

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public boolean equals(Cursor cursor) {
        return cursor.x == x && cursor.y == y;
    }
}