import java.io.*;
import java.util.*;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import library.core.*;

class Editor extends PComponent {
    private List<String> content;

    private File file;
    private boolean fileSaved = true;

    // Modes
    private Mode mode;
    private boolean multiCursor;

    // Viewport
    private PVector defaultViewportOffset;
    private PVector viewportOffset;

    // Cursors - Store the cursor positions
    private List<Cursor> cursors;
    private List<Cursor> activeCursors;
    private int cursorBlinkSpeed = 300;
    private int lastBlink;

    private boolean showLineNumbers = true;
    private boolean relativeLineNumbers = true;

    // Properties
    private color backgroundColor, textColor, currentLineColor, cursorColor, highlightColor;
    private int fontSize = 20;
    private String fontFamily = "Arial";
    private int tabSize = 4;

    private float lineHeight;

    // Bottom information section
    private float bottomMargin;
    private String errorMessage = "";

    // Motions
    private String motion = "";
    private String previousMotion = "";
    // TODO - add "u"
    private char[] operators = { 'c', 'd', 'y', 'i', 'a', 'f', 'F', 'r' }; // TODO - add g, <, >, z
    private char[] motions = { 'i', 'a', 'I', 'A', 'w', 'b', 'W', 'B', 'C', 'D', 'e', 'E', 'h', 'j', 'k', 'l', '%', '0',
            '_', '^', '$',
            'G', 's', 'p', 'P', 'x', 'o', 'O' };
    private char[] commands = { ':', '/', '?', '*' }; // TODO - add others?

    public Editor() {
        content = new ArrayList<>();
        content.add("");

        readProperties();

        mode = Mode.NORMAL;

        cursors = new ArrayList<>(1);
        createCursor();
        activeCursors = new ArrayList<>(1);
        activeCursors.add(cursors.get(0));

        noStroke();
        textSize(fontSize);
        textFont(fontFamily);
        textAlign(TextAlignment.LEFT);

        lineHeight = textHeight("A");
        bottomMargin = lineHeight * 3.3f;

        lastBlink = millis();

        defaultViewportOffset = PVector.zero();
        if (showLineNumbers)
            defaultViewportOffset.x = -textWidth("000 ");

        viewportOffset = defaultViewportOffset.copy();
    }

    private void readProperties() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("settings.properties"));

            backgroundColor = color(properties.getProperty("backgroundColor"));
            textColor = color(properties.getProperty("textColor"));
            currentLineColor = color(properties.getProperty("currentLineColor"));
            cursorColor = color(properties.getProperty("cursorColor"));
            highlightColor = color(properties.getProperty("highlightColor"));

            fontSize = parseInt(properties.getProperty("fontSize"));
            fontFamily = properties.getProperty("fontFamily");
            tabSize = parseInt(properties.getProperty("tabSize"));

            cursorBlinkSpeed = parseInt(properties.getProperty("cursorBlinkSpeed"));

            showLineNumbers = bool(properties.getProperty("showLineNumbers"));
            relativeLineNumbers = bool(properties.getProperty("relativeLineNumbers"));
        } catch (IOException e) {
        }
    }

    private void createCursor() {
        cursors.add(new Cursor());
        cursors.get(cursors.size() - 1).setContent(content);
    }

    private void createCursor(int x, int y) {
        cursors.add(new Cursor(x, y));
        cursors.get(cursors.size() - 1).setContent(content);
    }

    /**
     * Called when you know the user pressed a key and it should be added to the
     * content.
     */
    public void writeKey() {
        char keyToWrite = key;
        switch (keyString) {
            case "Backspace":
                // TODO - this will probably break with multi cursors cause if you delete a line
                // then the next cursor will be on the wrong line
                if (keysPressed.contains("Ctrl")) {
                    for (Cursor cursor : activeCursors) {
                        int previousX = cursor.x;
                        int previousY = cursor.y;
                        cursor.previousWord();
                        if (previousX == cursor.x && previousY == cursor.y)
                            continue;

                        int x = cursor.x;
                        int y = cursor.y;

                        if (x == 0) {
                            content.set(y - 1, content.get(y - 1) + content.get(y));
                            content.remove(y);
                            cursor.y--;
                            cursor.x = content.get(y - 1).length();
                            return;
                        }

                        content.set(y, content.get(y).substring(0, x - 1) + content.get(y).substring(x));
                    }

                    return;
                }
                for (Cursor cursor : activeCursors) {
                    int x = parseInt(cursor.x);
                    int y = parseInt(cursor.y);
                    if (x == 0 && y == 0)
                        continue;

                    if (x == 0) {
                        content.set(y - 1, content.get(y - 1) + content.get(y));
                        content.remove(y);
                        cursor.y--;
                        cursor.x = content.get(y - 1).length();
                        return;
                    }

                    content.set(y, content.get(y).substring(0, x - 1) + content.get(y).substring(x));
                    cursor.x--;
                }
                return;
            case "Enter":
                for (Cursor cursor : activeCursors) {
                    int x = parseInt(cursor.x);
                    int y = parseInt(cursor.y);
                    content.add(y + 1, content.get(y).substring(x));
                    content.set(y, content.get(y).substring(0, x));
                    cursor.y++;
                    cursor.x = 0;
                }
                return;
            case "Tab":
                keyToWrite = '\t';
                // TODO #1 keyPressed does not even get called when tab is pressed
                break;
            case "Shift":
                return;
            case "Ctrl":
                return;
            case "Caps Lock":
                return;
            case "Alt":
                return;
            case "Delete":
                return;
        }

        for (Cursor cursor : activeCursors) {
            int x = parseInt(cursor.x);
            int y = parseInt(cursor.y);
            content.set(y, content.get(y).substring(0, x) + keyToWrite + content.get(y).substring(x));
            cursor.x++;
        }

        fileSaved = false;
    }

    public void handleInsertMode() {
        if (keyString.equals("Escape")) {
            mode = Mode.NORMAL;
            return;
        }

        if (keyString.equals("Left")) {
            for (Cursor cursor : activeCursors)
                cursor.left();
            return;
        }
        if (keyString.equals("Right")) {
            for (Cursor cursor : activeCursors)
                cursor.right();
            return;
        }
        if (keyString.equals("Up")) {
            for (Cursor cursor : activeCursors)
                cursor.up();
            return;
        }
        if (keyString.equals("Down")) {
            for (Cursor cursor : activeCursors)
                cursor.down();
            return;
        }

        writeKey();
    }

    private boolean isOperator(char c) {
        for (int i = 0; i < operators.length; i++)
            if (c == operators[i])
                return true;

        return false;
    }

    private boolean isMotion(char c) {
        for (int i = 0; i < motions.length; i++)
            if (c == motions[i])
                return true;

        return false;
    }

    private boolean isCommand(char c) {
        for (int i = 0; i < commands.length; i++)
            if (c == commands[i])
                return true;

        return false;
    }

    private void saveFile() {
        if (file == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save text file");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int userSelection = fileChooser.showSaveDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                file = new File(fileChooser.getSelectedFile().getAbsolutePath());
                String name = file.getName();
                int index = name.lastIndexOf('.');
                if (index > 0)
                    name = name.substring(0, index);
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                setTitle(name);
            } else {
                return;
            }
        }

        try {
            FileWriter fileWriter = new FileWriter(file);
            for (String line : content)
                fileWriter.write(line + "\n");

            fileWriter.close();
        } catch (IOException e) {
            println("Unable to save file: " + e.getMessage());
        }

        fileSaved = true;
    }

    private void loadFileContents() {
        content = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine())
                content.add(scanner.nextLine());

            // Reset cursor
            cursors = new ArrayList<>();
            cursors.add(new Cursor(0, 0));
            activeCursors = new ArrayList<>();
            activeCursors.add(cursors.get(0));

            scanner.close();
        } catch (FileNotFoundException e) {
            println("Unable to open file: " + e.getMessage());
        }
    }

    private void openExplorer() {
        // Option to open a file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open text file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));

        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            String name = file.getName();
            int index = name.lastIndexOf('.');
            if (index > 0)
                name = name.substring(0, index);
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            setTitle(name);
            loadFileContents();
        }
    }

    private boolean parseCommandColon(String motion) {
        switch (motion) {
            case "w":
                saveFile();
                errorMessage = "File saved!";
                return true;
            case "wq":
                saveFile();
                exit();
                return true;
            case "q":
                if (!fileSaved) {
                    errorMessage = "Error: File not saved (press a command or enter to continue)";
                    return true; // Remove the motion
                }

                exit();
                return true;
            case "q!":
                exit();
                return true;
            case "E":
                openExplorer();
                return true;
        }

        return false;
    }

    private boolean parseCommandStar(String motion) {
        // TODO - implement
        // Simply a redirection to select the current word and then slash it
        return true;
    }

    private boolean parseCommandSlash(String motion) {
        // TODO - implement
        return true;
    }

    // :, *, /, etc.
    private boolean parseCommand() {
        String motion = this.motion;
        char command = motion.charAt(0);
        motion = motion.substring(1);

        switch (command) {
            case ':':
                return parseCommandColon(motion);
            case '*':
                return parseCommandStar(motion);
            case '/':
                return parseCommandSlash(motion);
        }

        return false;
    }

    private boolean runMotion(Cursor cursor, char motion) {
        switch (motion) {
            case 'i':
                mode = Mode.INSERT;
                return true;
            case 'a':
                mode = Mode.INSERT;
                cursor.right();
                return true;
            case 'I':
                cursor.findFirstNonWhitespace();
                mode = Mode.INSERT;
                return true;
            case 'A':
                cursor.findLastNonWhitespace();
                mode = Mode.INSERT;
                return true;
            case 'w':
                cursor.nextWord();
                return true;
            case 'b':
                cursor.previousWord();
                return true;
            case 'W':
                cursor.nextWordWithPunctuation();
                return true;
            case 'B':
                cursor.previousWordWithPunctuation();
                return true;
            case 'C':
                cursor.deleteToLineEnd();
                mode = Mode.INSERT;
                fileSaved = false;
                return true;
            case 'D':
                cursor.deleteToLineEnd();
                fileSaved = false;
                return true;
            case 'e':
                cursor.endOfWord();
                return true;
            case 'E':
                cursor.endOfWordWithPunctuation();
                return true;
            case 'h':
                cursor.left();
                return true;
            case 'j':
                cursor.down();
                return true;
            case 'k':
                cursor.up();
                return true;
            case 'l':
                cursor.right();
                return true;
            case '%':
                cursor.findMatchingBracket();
                return true;
            case '0':
                cursor.x = 0;
                return true;
            case '_':
                cursor.findFirstNonWhitespace();
                return true;
            case '^':
                cursor.findFirstNonWhitespace();
                return true;
            case '$':
                cursor.x = cursor.getEndOfLine();
                return true;
            case 'G':
                cursor.y = content.size() - 1;
                cursor.x = cursor.getEndOfLine();
                return true;
            case 's':
                cursor.deleteCurrentCharacter();
                mode = Mode.INSERT;
                fileSaved = false;
                return true;
            case 'p':
                cursor.pasteAfter();
                return true;
            case 'P':
                cursor.pasteBefore();
                return true;
            case 'x':
                cursor.deleteCurrentCharacter();
                fileSaved = false;
                return true;
            case 'o':
                cursor.newLineBelow();
                mode = Mode.INSERT;
                fileSaved = false;
                return true;
            case 'O':
                cursor.newLineAbove();
                mode = Mode.INSERT;
                fileSaved = false;
                return true;
        }

        return false;
    }

    // w, 3b, etc.
    private boolean runMotion(int numTimes, char motion) {
        for (Cursor cursor : activeCursors) {
            for (int i = 0; i < numTimes; i++) {
                boolean result = runMotion(cursor, motion);
                if (!result)
                    return false;
            }
        }

        return true;
    }

    // dw, d3w, 3cw, etc.
    private boolean runMotion(int numTimesTotal, char operator, int numTimes, char motion) {
        // TODO - implement
        switch (operator) {
            case 'c':
            case 'd':
            case 'y':
            case 'i':
            case 'a':
            case 'f':
            case 'F':
            case 'r':
        }
        return false;
    }

    // dd, 3yy, 2d2d
    private boolean runOperator(int numTimesTotal, char firstOperator, int numTimes, char secondOperator) {
        // Operators must be the same
        if (firstOperator != secondOperator)
            return false;

        // TODO - implement
        return false;
    }

    // ciw, etc.
    private boolean runMotion(int numTimesTotal, char mainOperator, int numTimes, char secondOperator, char motion) {
        // TODO - implement
        return false;
    }

    private void parseMotion() {
        if (motion.length() == 0)
            return;

        // Structure:
        // (number?)(motion). Ex: 3w, $
        // (number?)(operator)(number?)(motion). Ex: 3dw, 2d3w, 2d$, d$, d5$
        // (number?)(operator)(number?)(operator). Ex: 3dd, yy
        // (number?)(operator)(number?)(operator)(motion). Ex: ciw
        String motion = this.motion;

        // First check to see if we have a number
        int number = 0;
        while (motion.length() > 0 && Character.isDigit(motion.charAt(0))) {
            number = number * 10 + parseInt(motion.substring(0, 1));
            motion = motion.substring(1);
        }
        if (number == 0)
            number = 1;

        // Structure:
        // (motion). Ex: 3w, $
        // (operator)(number?)(motion). Ex: 3dw, 2d3w, 2d$, d$, d5$
        // (operator)(number?)(operator). Ex: 3dd, yy
        // (operator)(number?)(operator)(motion). Ex: ciw
        if (motion.length() == 0)
            return;

        // Check if we have a motion (1st structure)
        char c = motion.charAt(0);
        if (isMotion(c)) {
            if (runMotion(number, c))
                this.motion = "";
            return;
        }

        // If it's not an operator, then we don't have a valid motion
        if (!isOperator(c))
            return;

        char operator = c;
        motion = motion.substring(1);

        // Structure:
        // (number?)(motion). Ex: 3dw, 2d3w, 2d$, d$, d5$
        // (number?)(operator). Ex: 3dd, yy
        // (number?)(operator)(motion). Ex: ciw

        int number2 = 1;
        while (motion.length() > 0 && Character.isDigit(motion.charAt(0))) {
            number2 = number2 * 10 + parseInt(motion.substring(0, 1));
            motion = motion.substring(1);
        }

        if (motion.length() == 0) {
            // Example: 3d3 or 3d meaning it's not a valid motion
            return;
        }

        // Structure:
        // (motion). Ex: 3dw, 2d3w, 2d$, d$, d5$
        // (operator). Ex: 3dd, yy
        // (operator)(motion). Ex: ciw

        char c2 = motion.charAt(0);
        if (isMotion(c2)) {
            if (runMotion(number, operator, number2, c2))
                this.motion = "";
            return;
        }

        // Structure:
        // (operator). Ex: 3dd, yy
        // (operator)(motion). Ex: ciw

        if (!isOperator(c2))
            return;

        char operator2 = c2;
        motion = motion.substring(1);

        if (motion.length() == 0) {
            // Example: 3dd or 3d3$, so it's valid
            if (runOperator(number, operator, number2, operator2))
                this.motion = "";
            return;
        }

        // Structure:
        // (motion)

        char motion2 = motion.charAt(0);
        if (runMotion(number, operator, number2, operator2, motion2))
            this.motion = "";

    }

    private boolean handleMotions() {
        if (keyString.equals("Escape")) {
            motion = "";
            return true;
        }
        if (keyString.equals("Backspace")) {
            if (motion.length() > 0) {
                motion = motion.substring(0, motion.length() - 1);
            } else {
                for (Cursor cursor : activeCursors)
                    cursor.left();
            }
            return true;
        }
        if (errorMessage.length() > 0 && keyString.equals("Enter")) {
            errorMessage = "";
            return true;
        }
        if (motion.length() > 0 && keyString.equals("Enter")) {
            if (isCommand(motion.charAt(0))) {
                if (parseCommand())
                    motion = "";
                return true;
            }
        }

        String[] keysToIgnore = { "Shift", "Enter", "Tab", "Backspace", "Delete", "Control", "Alt", "Caps Lock" };
        for (String key : keysToIgnore) {
            if (keyString.equals(key))
                return false;
        }

        motion += key;
        if (errorMessage.length() > 0)
            errorMessage = "";
        parseMotion();
        return true;
    }

    public boolean handleNormalMode() {
        if (key == 'v') {
            mode = Mode.VISUAL;
            for (int i = 0; i < cursors.size(); i++) {
                Cursor cursor = cursors.get(i);
                if (isActive(cursor)) {
                    Cursor newCursor = new Cursor(cursor.x, cursor.y);
                    newCursor.setContent(content);
                    cursors.add(i + 1, newCursor);
                }
            }
            return true;
        } else if (key == 'V') {
            mode = Mode.VISUAL;
            for (int i = 0; i < cursors.size(); i++) {
                Cursor cursor = cursors.get(i);
                if (isActive(cursor)) {
                    cursor.x = 0; // Go to the beginning of the line
                    Cursor newCursor = new Cursor(cursor.x, cursor.y);
                    newCursor.setContent(content);
                    newCursor.x = content.get(cursor.y).length(); // Go to the end of the line
                    cursors.add(i + 1, newCursor);
                }
            }
            return true;
        }

        return handleMotions();
    }

    public boolean handleVisualMode() {
        // Escape and they aren't typing a motion right now
        if (keyString.equals("Escape") && motion.length() == 0) {
            mode = Mode.NORMAL;
            removeNonActiveCursors();
            return true;
        }

        return handleMotions();
    }

    public void keyPressed() {
        switch (mode) {
            case INSERT:
                handleInsertMode();
                lastBlink = millis();
                for (Cursor cursor : activeCursors)
                    cursor.makeVisible();
                break;
            case NORMAL:
                if (handleNormalMode()) {
                    lastBlink = millis();
                    for (Cursor cursor : activeCursors)
                        cursor.makeVisible();
                }
                break;
            case VISUAL:
                if (handleVisualMode()) {
                    lastBlink = millis();
                    for (Cursor cursor : activeCursors)
                        cursor.makeVisible();
                }
                break;
        }

        for (Cursor cursor : cursors)
            cursor.setContent(content);
    }

    private void updateViewportOffset() {
        PVector cursorPos = activeCursors.get(0).getPos();
        // If cursorPos.y is less than the viewportOffset.y, then we need to move the
        // viewport up
        // println(cursorPos.y, viewportOffset.y);
        if (cursorPos.y < viewportOffset.y) {
            viewportOffset.y = cursorPos.y;
        }

        // If cursorPos.y is greater than the viewportOffset.y + height, then we need to
        // move the viewport down
        float maxY = height - bottomMargin + viewportOffset.y;
        if (cursorPos.y + lineHeight > maxY) {
            float diff = maxY - (cursorPos.y + lineHeight);
            viewportOffset.y -= diff;
        }

        // TODO - handle horizontal scrolling
    }

    public void drawInformationSection() {
        push();
        resetTranslation();

        // Draw bottom two lines
        // (file path) on left, on right: line number, column number, percentage of file
        // (mode if not in normal), on right: motion being typed
        String filePath = "[No name]";
        if (file != null) {
            filePath = file.getAbsolutePath();

            if (!fileSaved)
                filePath += " [+]";
        }

        fill(backgroundColor);
        rect(0, height - bottomMargin, width, bottomMargin);
        fill(255, 35);
        rect(0, height - bottomMargin, width, lineHeight);

        translate(0, height - bottomMargin + lineHeight / 2);
        fill(textColor);
        text(filePath, 5, 0);

        Cursor cursor = activeCursors.get(0);
        String position = cursor.y + 1 + "," + cursor.x;
        text(position, width * 0.8, 0);

        int percent = (int) map(cursor.y, 0, content.size() - 1, 0, 100);
        String percentage = str(percent);
        if (percentage.equals("0"))
            percentage = "Top";
        else if (percentage.equals("100"))
            percentage = "Bot";
        else
            percentage += "%";
        // TODO make this based on the viewport's y instead of the cursor position.

        textAlign(TextAlignment.RIGHT);
        text(percentage, width - textWidth(percentage) / 2, 0);
        textAlign(TextAlignment.LEFT);

        translate(0, lineHeight - 3);
        if (errorMessage.length() > 0) {
            text(errorMessage, 5, 0);
        } else {
            if (motion.length() == 0) {
                String modeString = "";
                switch (mode) {
                    case NORMAL:
                        break;
                    case INSERT:
                        modeString = "-- INSERT --";
                        break;
                    case VISUAL:
                        modeString = "-- VISUAL --";
                        break;
                }

                text(modeString, 5, 0);
            } else
                text(motion, 5, 0);
        }

        pop();
    }

    private List<PVector> getSelectedCharacters() {
        if (mode != Mode.VISUAL)
            return new ArrayList<PVector>();

        List<PVector> selectedCharacters = new ArrayList<PVector>();
        for (int i = 0; i < cursors.size(); i++) {
            if (!isActive(cursors.get(i)))
                continue;

            // TODO - is this bad for performance?
            Cursor start = cursors.get(i);
            Cursor end = cursors.get(i + 1);
            if (cursors.get(i).toRightOf(cursors.get(i + 1))) {
                start = cursors.get(i + 1);
                end = cursors.get(i);
            }

            Cursor cursor = new Cursor(start.x, start.y);
            cursor.setContent(content);
            // While it hasn't reached the end, add the character to the list and then move
            // it to the right
            while (!cursor.equals(end)) {
                selectedCharacters.add(new PVector(cursor.x, cursor.y));
                cursor.right();
            }
            selectedCharacters.add(new PVector(cursor.x, cursor.y)); // Add the last character
        }

        return selectedCharacters;
    }

    private boolean isActive(Cursor cursor) {
        return activeCursors.contains(cursor);
    }

    private void removeNonActiveCursors() {
        for (int i = cursors.size() - 1; i >= 0; i--) {
            Cursor cursor = cursors.get(i);
            if (!activeCursors.contains(cursor))
                cursors.remove(i);
        }
    }

    private void updateCursors() {
        for (Cursor cursor : cursors) {
            if (!activeCursors.contains(cursor))
                cursor.makeInvisible();
        }
    }

    public void draw() {
        updateViewportOffset();
        translate(PVector.mult(viewportOffset, -1)); // -1 cause if the viewport is looking 300 down, we need to move
                                                     // the content up 300
        updateCursors();

        background(backgroundColor);

        // Line numbers
        float brightness = brightness(textColor);
        brightness *= 0.7;

        if (showLineNumbers) {
            push();
            translate(viewportOffset.x, 0);
            for (int i = 0; i < content.size(); i++) {
                int lineNumber = i + 1;
                if (relativeLineNumbers) {
                    lineNumber = abs((activeCursors.get(0).y - i));
                    if (i == (activeCursors.get(0)).y)
                        lineNumber = i + 1;
                }

                if (i == (activeCursors.get(0)).y)
                    fill(currentLineColor);
                else
                    fill(color.fromHSB(hue(textColor), saturation(textColor), brightness));

                text(lineNumber, 0, lineHeight / 2);
                translate(0, lineHeight);
            }
            pop();

            push();
            translate(0, viewportOffset.y);
            stroke(color.fromHSB(hue(textColor), saturation(textColor), brightness));
            strokeWeight(1);
            line(-3, 0, -3, height - bottomMargin);
            pop();
        }

        // Draw cursors
        // Toggle cursor visibility
        if (millis() - lastBlink > cursorBlinkSpeed) {
            lastBlink = millis();
            for (Cursor cursor : activeCursors)
                cursor.toggleVisibility();
        }

        fill(cursorColor);
        for (Cursor cursor : cursors) {
            cursor.draw(mode);
        }

        // Draw text
        fill(textColor);
        push();
        List<PVector> selectedCharacters = getSelectedCharacters();
        for (int i = 0; i < content.size(); i++) {
            push();
            String line = content.get(i);
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                if (selectedCharacters.contains(new PVector(j, i))) {
                    fill(highlightColor);
                    float rectSize = textWidth(c + "");
                    if (c == '\t')
                        rectSize = tabSize * textWidth(" ");
                    rect(0, 0, rectSize, lineHeight);
                    fill(textColor);
                }
                if (c == '\t') {
                    translate(tabSize * textWidth(" "), 0);
                } else {
                    text(c, 0, lineHeight / 2);
                    translate(textWidth(c + ""), 0);
                }
            }
            pop();
            translate(0, lineHeight);
        }
        pop();

        drawInformationSection();
    }
}