import java.util.*;
import library.core.*;

public class Cursor extends PComponent implements EventIgnorer {
    private enum BracketType {
        CURLY, SQUARE, PARENTHESIS, TAG, NONE
    }

    int x;
    int y;

    private Editor editor;
    private ArrayList<String> content;

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

    public List<String> getContent() {
        return content;
    }

    public void setContent(ArrayList<String> content) {
        this.content = content;
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

    public void constrain() {
        y = min(y, content.size() - 1);
        y = max(y, 0);
        x = min(x, getEndOfLine());
        x = max(x, 0);
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
            right();

        // Go back one. Don't only if I'm at the end of the line and on the same type
        if (!(isEndOfLine() && getCharType(x, y) == type))
            left();
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

    /**
     * Moves the cursor to the start of the current range it's in. <br>
     * <br>
     * For example:<br>
     * <br>
     * if (cursor in here) {<br>
     * <br>
     * running this function with the arg '(' would move the cursor to the "("
     */
    public void startOfRange(char openingBracket) {
        PVector startingPos = toPVector();

        char closingBracket = getMatchingBracket(openingBracket);
        int brackets = -1; // -1 for closing, 1 for opening, stop when 0 && on opening bracket
        // Start at -1 so that when we find the opening bracket, we're at 0
        // very dangerous
        while (true) {
            char currentChar = getCurrentChar();
            if (currentChar == openingBracket)
                brackets++;
            else if (currentChar == closingBracket && !toPVector().equals(startingPos)) // don't count the starting
                                                                                        // closing bracket
                brackets--;

            if (brackets == 0 && currentChar == openingBracket)
                return;

            PVector previousPos = toPVector();
            left();
            // At the start of the file, didn't find it
            if (previousPos.equals(toPVector())) {
                setPVector(startingPos);
                return;
            }
        }
    }

    /**
     * Moves to the start of the next range.
     */
    public void nextRange(char openingBracket) {
        PVector startingPos = toPVector();
        while (!isEndOfContent()) {
            char currentChar = getCurrentChar();
            if (currentChar == openingBracket) {
                return;
            }

            right();
        }

        // Didn't find, go back to where we started
        setPVector(startingPos);
    }

    public void startOfCurrentWord() {
        CharType charType = getCharType(x, y);
        while (x > 0 && getCharType(x - 1, y) == charType)
            left();
    }

    public void endOfCurrentWord() {
        CharType charType = getCharType(x, y);
        while (x < getEndOfLine() && getCharType(x + 1, y) == charType)
            right();
    }

    public void deleteCurrentCharacter() {
        if ((!onCharacter() && x != 0) || x >= content.get(y).length())
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
        char bracketChar = getCurrentChar();
        if (bracketChar == Character.MIN_VALUE)
            return;
        boolean isOpeningBracket = isOpeningBracket(bracketChar);
        int bracketCount = isOpeningBracket ? 1 : -1;

        int initialX = x;
        int initialY = y;
        boolean firstRound = true;
        while (bracketCount != 0) {
            BracketType currentBracketType = getBracketType(x, y);
            if (currentBracketType == bracketType) {
                if (firstRound) {
                    firstRound = false;
                } else {
                    if (isOpeningBracket(content.get(y).charAt(x)))
                        bracketCount++;
                    else
                        bracketCount--;
                }
            }

            // We found the matching bracket
            if (bracketCount == 0)
                return;

            int previousX = x;
            int previousY = y;
            if (isOpeningBracket)
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
        if (data == null)
            return;
        String[] lines = data.split("\n");
        for (int i = 0; i < lines.length; i++) {
            content.add(y + i + 1, lines[i]);
        }
        y += lines.length;
        x = getEndOfLine();
    }

    public void pasteBefore() {
        String data = getClipboardContents();
        if (data == null)
            return;
        String[] lines = data.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int index = y - (lines.length - i);
            index = max(0, index);
            content.add(index, lines[i]);
        }
        y += lines.length - 1;
        x = getEndOfLine();
    }

    public void joinLines() {
        if (y >= content.size() - 1)
            return;

        // Get line below
        String lineBelow = content.get(y + 1);
        lineBelow = lineBelow.substring(findFirstNonWhitespace(lineBelow));

        // Move cursor to middle of the two joined lines
        x = findLastNonWhitespace(content.get(y));

        // Join the lines (check to see if current line is empty, if so don't add a
        // space at the start)
        if (content.get(y).length() == 0)
            content.set(y, lineBelow);
        else
            content.set(y, content.get(y) + " " + lineBelow);
        content.remove(y + 1);
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

    public CharType getCharType(int x, int y) {
        if (y >= content.size() || y < 0 || x >= content.get(y).length() || x < 0 || !onCharacter())
            return null;
        char c = getChar(x, y);

        if (Character.isLetter(c))
            return CharType.LETTER;
        if (Character.isDigit(c))
            return CharType.NUMBER;
        if (c == ' ')
            return CharType.SPACE;

        return CharType.PUNCTUATION;
    }

    private BracketType getBracketType(int x, int y) {
        if (!onCharacter())
            return BracketType.NONE;
        char c = getCurrentChar();

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

    private char getMatchingBracket(char c) {
        switch (c) {
            case '(':
                return ')';
            case ')':
                return '(';
            case '[':
                return ']';
            case ']':
                return '[';
            case '{':
                return '}';
            case '}':
                return '{';
            case '<':
                return '>';
            case '>':
                return '<';
            default:
                return Character.MIN_VALUE;
        }
    }

    public boolean isEndOfLine() {
        if (content.get(y).length() == 0)
            return true;
        return x == getEndOfLine();
    }

    private boolean isInLine() {
        return !isEndOfLine();
    }

    public int getEndOfLine() {
        if (y >= content.size() || y < 0)
            return 0; // probably not ideal to return 0 instead of -1, but should avoid errors
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

    public boolean onCharacter() {
        try {
            content.get(y).charAt(x);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private char getChar(int x, int y) {
        try {
            return content.get(y).charAt(x);
        } catch (Exception e) {
            return Character.MIN_VALUE;
        }
    }

    public char getCurrentChar() {
        return getChar(x, y);
    }

    public boolean toRightOf(Cursor other) {
        if (y > other.y)
            return true;

        if (y < other.y)
            return false;

        return x > other.x;
    }

    public void fixOutOfBounds() {
        y = max(0, min(y, content.size() - 1));
        x = max(0, min(x, getEndOfLine()));
    }

    public String getWordWithPunctuation() {
        CharType charType = getCharType(x, y);
        if (charType == CharType.SPACE || charType == null)
            return "";

        Cursor position = copy(); // Save the position of the cursor to restore it later

        // Move to the start of the word
        if (x > 0) {
            if (getCharType(x - 1, y) != CharType.SPACE) {
                previousWordWithPunctuation();
            }
        }

        // Find index of next space
        int nextSpace = content.get(y).indexOf(' ', x);
        if (nextSpace == -1)
            nextSpace = content.get(y).length();

        String word = content.get(y).substring(x, nextSpace);
        x = position.x;
        y = position.y;
        return word;
    }

    public String getWord() {
        CharType charType = getCharType(x, y);
        if (charType == CharType.SPACE || charType == null)
            return "";

        Cursor position = copy();

        startOfCurrentWord();

        int startX = x;
        // Move to the end of the word (unless we are already at the end of the line)
        if (x < getEndOfLine() && getCharType(x + 1, y) != CharType.SPACE)
            endOfWord();

        String word = content.get(y).substring(startX, (int) toPVector().x + 1);

        x = position.x;
        y = position.y;
        return word;
    }

    public boolean isOnLink() {
        String word = getWordWithPunctuation();
        return isValidURL(word);
    }

    private PVector getTextPosition(int x, int y) {
        float textHeight = textAscent() + textDescent();
        float yPosition = textHeight * y;
        if (content.get(y).length() == 0)
            return new PVector(0, yPosition);

        return new PVector(textWidth(content.get(y).substring(0, x)), yPosition);
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

    public PVector toPVector() {
        return new PVector(x, y);
    }

    public void setPVector(PVector pos) {
        x = (int) pos.x;
        y = (int) pos.y;
    }

    public void draw(Mode mode) {
        content = editor.getContent();
        clamp(mode);

        if (!visible)
            return;

        PVector pos = getPos();
        float lineHeight = textAscent() + textDescent();
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

    public Cursor copy() {
        return new Cursor(editor, x, y);
    }
}