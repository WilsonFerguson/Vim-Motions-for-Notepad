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

    // Viewport
    private PVector defaultViewportOffset;
    private PVector viewportOffset;

    // Cursor
    private Cursor cursor;
    private int cursorBlinkSpeed = 300;
    private int lastBlink;

    // Visual mode
    private List<PVector> visualEndpoints = new ArrayList<>();
    private int visualSelectionIndex = 0;

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

        cursor = new Cursor(this);

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

        setTitle("Untitled");
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

            rectMode(CORNER);
        } catch (IOException e) {
        }
    }

    public List<String> getContent() {
        return content;
    }

    public Mode getMode() {
        return mode;
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
                    int previousX = cursor.x;
                    int previousY = cursor.y;
                    cursor.previousWord();
                    if (previousX == cursor.x && previousY == cursor.y)
                        return;

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

                    return;
                }
                int x = parseInt(cursor.x);
                int y = parseInt(cursor.y);
                if (x == 0 && y == 0)
                    return;

                if (x == 0) {
                    int originalLength = content.get(y - 1).length();
                    content.set(y - 1, content.get(y - 1) + content.get(y));
                    content.remove(y);
                    cursor.y--;
                    cursor.x = originalLength;
                    return;
                }

                content.set(y, content.get(y).substring(0, x - 1) + content.get(y).substring(x));
                cursor.x--;
                fileSaved = false;
                return;
            case "Enter":
                int previousX = parseInt(cursor.x);
                int previousY = parseInt(cursor.y);
                content.add(previousY + 1, content.get(previousY).substring(previousX));
                content.set(previousY, content.get(previousY).substring(0, previousX));
                cursor.y++;
                cursor.x = 0;
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

        int x = parseInt(cursor.x);
        int y = parseInt(cursor.y);
        content.set(y, content.get(y).substring(0, x) + keyToWrite + content.get(y).substring(x));
        cursor.x++;

        fileSaved = false;
    }

    public void handleInsertMode() {
        if (keyString.equals("Escape")) {
            mode = Mode.NORMAL;
            return;
        }

        if (keyString.equals("Left")) {
            cursor.left();
            return;
        }
        if (keyString.equals("Right")) {
            cursor.right();
            return;
        }
        if (keyString.equals("Up")) {
            cursor.up();
            return;
        }
        if (keyString.equals("Down")) {
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
                addToRecentFiles();
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

            cursor = new Cursor(this);

            scanner.close();
        } catch (FileNotFoundException e) {
            println("Unable to open file: " + e.getMessage());
        }
    }

    public void openExplorer() {
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
            addToRecentFiles();
        }
    }

    private void addToRecentFiles() {
        if (file == null)
            return;

        String[] recentFiles = loadStrings("RecentFiles.txt");
        List<String> recentFilesList = new ArrayList<>(Arrays.asList(recentFiles));
        // Check if file is already in the list, if so remove it
        for (int i = recentFilesList.size() - 1; i >= 0; i--)
            if (recentFilesList.get(i).equals(file.getAbsolutePath()))
                recentFilesList.remove(i);
        recentFilesList.add(0, file.getAbsolutePath());

        Set<String> recentFilesSet = new HashSet<>();
        for (int i = recentFilesList.size() - 1; i >= 0; i--) {
            if (recentFilesSet.contains(recentFilesList.get(i)))
                recentFilesList.remove(i);
            else
                recentFilesSet.add(recentFilesList.get(i));
        }

        if (recentFilesList.size() > 9)
            recentFilesList = recentFilesList.subList(0, 9);

        String[] recentFilesArray = new String[recentFilesList.size()];
        recentFilesList.toArray(recentFilesArray);
        saveStrings(recentFilesArray, "RecentFiles.txt");
    }

    public void setFile(File file) {
        this.file = file;
        String name = file.getName();
        int index = name.lastIndexOf('.');
        if (index > 0)
            name = name.substring(0, index);
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        setTitle(name);
        loadFileContents();
        addToRecentFiles();
    }

    private boolean parseCommandColon(String motion) {
        switch (motion) {
            case "w":
                saveFile();
                if (file != null)
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
        // Get the word at the cursor, set motion to "/word", return
        // parseCommandSlash(motion)
        // TODO - implement
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

    private boolean runMotion(char motion) {
        switch (motion) {
            case 'i':
                mode = Mode.INSERT;
                return true;
            case 'a':
                mode = Mode.INSERT;
                cursor.right();
                return true;
            case 'I':
                mode = Mode.INSERT;
                cursor.findFirstNonWhitespace();
                return true;
            case 'A':
                mode = Mode.INSERT;
                cursor.findLastNonWhitespace();
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
        for (int i = 0; i < numTimes; i++) {
            boolean result = runMotion(motion);
            if (!result)
                return false;
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
            visualEndpoints.clear();
            visualEndpoints.add(cursor.copy().toPVector());
            visualEndpoints.add(cursor.copy().toPVector());
            visualSelectionIndex = 0;
            return true;
        } else if (key == 'V') {
            mode = Mode.VISUAL;
            cursor.x = cursor.getEndOfLine();
            visualEndpoints.clear();
            visualEndpoints.add(new PVector(0, cursor.copy().y));
            visualEndpoints.add(cursor.copy().toPVector());
            visualSelectionIndex = 1;
            return true;
        }

        return handleMotions();
    }

    public boolean handleVisualMode() {
        // Escape and they aren't typing a motion right now
        if (keyString.equals("Escape") && motion.length() == 0) {
            mode = Mode.NORMAL;
            visualEndpoints.clear();
            return true;
        }

        return handleMotions();
    }

    public void keyPressed() {
        switch (mode) {
            case INSERT:
                handleInsertMode();
                lastBlink = millis();
                cursor.makeVisible();
                break;
            case NORMAL:
                if (handleNormalMode()) {
                    lastBlink = millis();
                    cursor.makeVisible();
                }
                break;
            case VISUAL:
                if (handleVisualMode()) {
                    lastBlink = millis();
                    cursor.makeVisible();
                }
                break;
        }
    }

    // TODO - this is quite a hacky way to do this
    public void mimicKeyPress(char key) {
        char previousKey = this.key;
        PComponent.key = key;
        keyPressed();
        PComponent.key = previousKey;
    }

    private void updateViewportOffset() {
        PVector cursorPos = cursor.getPos();
        // If cursorPos.y is less than the viewportOffset.y, then we need to move the
        // viewport up
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

        }
        if (!fileSaved)
            filePath += " [+]";

        fill(backgroundColor);
        rect(0, height - bottomMargin, width, bottomMargin);
        fill(255, 20);
        rect(0, height - bottomMargin, width, lineHeight);

        translate(0, height - bottomMargin + lineHeight / 2);
        fill(textColor);
        text(filePath, 5, 0);

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

        List<PVector> selectedCharacters = new ArrayList<>();
        PVector start = visualEndpoints.get(0).copy();
        PVector end = visualEndpoints.get(1).copy();
        // Swap start and end if they're in the wrong order
        if (start.y > end.y || (start.y == end.y && start.x > end.x)) {
            PVector temp = start;
            start = end;
            end = temp;
        }

        Cursor pointer = new Cursor(this, (int) start.x, (int) start.y);
        while (pointer.x != end.x || pointer.y != end.y) {
            selectedCharacters.add(pointer.toPVector());
            pointer.right();
        }
        selectedCharacters.add(pointer.toPVector());

        return selectedCharacters;
    }

    private void drawContent() {
        PVector position = PVector.zero();

        float spaceWidth = textWidth(" ");
        float charWidth = textWidth("A");

        // Highlight the selected characters
        List<PVector> selectedCharacters = getSelectedCharacters();
        fill(highlightColor);
        for (PVector selectedCharacter : selectedCharacters) {
            // If selected character is below the viewport, ignore it
            if (selectedCharacter.y > viewportOffset.y + height - bottomMargin)
                continue;

            float rectSize = charWidth + 1;
            String line = content.get((int) selectedCharacter.y);
            if (line.length() > selectedCharacter.x && line.charAt((int) selectedCharacter.x) == '\t')
                rectSize = tabSize * spaceWidth;
            rect(selectedCharacter.x * charWidth, selectedCharacter.y * lineHeight, rectSize, lineHeight + 1);
        }

        // Draw cursors
        fill(cursorColor);
        cursor.draw(mode);

        // Draw the content line by line
        fill(textColor);
        for (String line : content) {
            // If position is below the viewport, stop drawing
            if (position.y > viewportOffset.y + height - bottomMargin)
                break;

            // Handle tabs
            if (line.contains("\t")) {
                String[] words = line.split("\t");
                float x = position.x;
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    text(word, x, position.y + lineHeight / 2);
                    x += textWidth(word);
                    if (i != words.length - 1) {
                        text(" ", x, position.y + lineHeight / 2);
                        x += spaceWidth * tabSize;
                    }
                }
            } else {
                text(line, position.x, position.y + lineHeight / 2);
            }

            // Move to the next line
            position.y += lineHeight;
        }
    }

    private void drawLineNumbers() {
        if (!showLineNumbers)
            return;

        float brightness = brightness(textColor);
        brightness *= 0.7;

        push();
        translate(viewportOffset.x, 0);

        for (int i = 0; i < content.size(); i++) {
            // If above the viewport, skip it
            if (getTranslation().y < -lineHeight) {
                translate(0, lineHeight);
                continue;
            }

            int lineNumber = i + 1;
            if (relativeLineNumbers) {
                lineNumber = abs(cursor.y - i);
                if (i == cursor.y)
                    lineNumber = i + 1;
            }

            if (i == cursor.y) {
                fill(currentLineColor);
                textAlign(LEFT);
                text(lineNumber, 0, lineHeight / 2);
            } else {
                fill(color.fromHSB(hue(textColor), saturation(textColor), brightness));
                textAlign(RIGHT);
                text(lineNumber, -viewportOffset.x - 4, lineHeight / 2);
            }

            translate(0, lineHeight);

            // If below the viewport, stop drawing
            if (getTranslation().y > height - bottomMargin)
                break;
        }
        pop();

        push();
        translate(0, viewportOffset.y);
        stroke(color.fromHSB(hue(textColor), saturation(textColor), brightness));
        strokeWeight(1);
        line(-1, 0, -1, height - bottomMargin);

        pop();
    }

    private void updateVisualEndpoints() {
        if (mode != Mode.VISUAL)
            return;

        // Shouldn't happen, but just to be safe
        if (visualEndpoints.size() == 0)
            return;

        visualEndpoints.set(visualSelectionIndex, cursor.toPVector());
    }

    public void draw() {
        updateViewportOffset();
        background(backgroundColor);
        translate(PVector.mult(viewportOffset, -1)); // -1 cause if the viewport is looking 300 down, we need to move
                                                     // the content up 300
        updateVisualEndpoints();

        drawLineNumbers();

        // Toggle cursor visibility
        if (cursorBlinkSpeed > 0 && millis() - lastBlink > cursorBlinkSpeed) {
            lastBlink = millis();
            cursor.toggleVisibility();
        }

        drawContent();
        drawInformationSection();
    }
}