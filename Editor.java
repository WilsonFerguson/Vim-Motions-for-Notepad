import java.io.*;
import java.util.*;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import library.core.*;

class Editor extends PComponent {
    private Sketch sketch;

    private ArrayList<String> content;
    private ArrayList<ArrayList<String>> history;
    private int historyIndex = -1; // Will get set to 0 after first history push

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
    private color backgroundColor, textColor, currentLineColor, cursorColor, highlightColor, linkColor;
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

    private char[] operatorsNormal = { 'c', 'd', 'y', 'r' };
    private char[] operatorsVisual = { 'y', 'i', 'a', 'r' };
    private char[] operatorsGeneric = { 'f', 'F' }; // TODO - add g, <, >, z

    private char[] motionsNormal = { 'i', 'a', 'C', 'D', 's', 'p', 'P', 'x', 'o', 'O' };
    private char[] motionsVisual = { 'c', 'd', 'C', 'D', 's', 'p', 'P', 'x', 'o', 'O' };
    private char[] motionsGeneric = { 'I', 'A', 'w', 'b', 'W', 'B', 'e', 'E', 'h', 'j', 'k', 'l', '%', '0', '_', '^',
            '$', 'G', '.', 'u' };

    private char[] commands = { ':', '/', '?', '*' };

    public Editor(Sketch sketch) {
        this.sketch = sketch;

        content = new ArrayList<>();
        content.add("");
        history = new ArrayList<>();
        pushToHistory();

        readProperties();

        mode = Mode.NORMAL;

        cursor = new Cursor(this);

        noStroke();
        textSize(fontSize);
        textFont(fontFamily);
        textAlign(TextAlignment.LEFT);

        lineHeight = textAscent() + textDescent();
        bottomMargin = lineHeight * 2;

        lastBlink = millis();

        defaultViewportOffset = PVector.zero();
        if (showLineNumbers)
            defaultViewportOffset.x = -textWidth("000 ");

        viewportOffset = defaultViewportOffset.copy();

        setTitle("Untitled");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
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
            linkColor = color(properties.getProperty("linkColor"));

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

    public ArrayList<String> getContent() {
        return content;
    }

    public Mode getMode() {
        return mode;
    }

    private void pushToHistory() {
        historyIndex++;
        history.add(max(historyIndex, 0), new ArrayList<>(content));
    }

    private void undo() {
        if (historyIndex == 0)
            return;

        historyIndex--;
        content = new ArrayList<>(history.get(historyIndex));
        cursor.setContent(content);
        cursor.fixOutOfBounds();
    }

    private void redo() {
        if (historyIndex == history.size() - 1)
            return;

        historyIndex++;
        content = new ArrayList<>(history.get(historyIndex));
        cursor.setContent(content);
        cursor.fixOutOfBounds();
    }

    /**
     * Called when you know the user pressed a key and it should be added to the
     * content.
     */
    public void writeKey() {
        char keyToWrite = key;
        switch (keyString) {
            case "Backspace":
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
            if (!history.get(historyIndex).equals(content))
                pushToHistory();
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
        for (char operator : operatorsGeneric)
            if (c == operator)
                return true;

        if (mode == Mode.VISUAL) {
            for (char operator : operatorsVisual)
                if (c == operator)
                    return true;
        } else if (mode == Mode.NORMAL) {
            for (char operator : operatorsNormal)
                if (c == operator)
                    return true;
        }

        return false;
    }

    private boolean isMotionGeneric(char c) {
        for (char motion : motionsGeneric)
            if (c == motion)
                return true;

        return false;
    }

    private boolean isOperatorGeneric(char c) {
        for (char operator : operatorsGeneric)
            if (c == operator)
                return true;

        return false;
    }

    private boolean isMotion(char c) {
        if (isMotionGeneric(c))
            return true;

        if (mode == Mode.VISUAL) {
            for (char motion : motionsVisual)
                if (c == motion)
                    return true;
        } else if (mode == Mode.NORMAL) {
            for (char motion : motionsNormal)
                if (c == motion)
                    return true;
        }

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

            pushToHistory();
        } catch (FileNotFoundException e) {
            println("Unable to open file: " + e.getMessage());
        }
    }

    public boolean openExplorer() {
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
            return true;
        }

        return false;
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
                // Only exit the program if they actually saved the file
                if (fileSaved)
                    sketch.setState(0);
                return true;
            case "q":
                if (!fileSaved) {
                    errorMessage = "Error: File not saved (press a command or enter to continue)";
                    return true; // Remove the motion
                }

                sketch.setState(0);
                return true;
            case "q!":
                sketch.setState(0);
                return true;
            case "wqa":
                saveFile();
                // Only exit the program if they actually saved the file
                if (fileSaved)
                    exit();

                return true;
            case "qa":
                if (!fileSaved) {
                    errorMessage = "Error: File not saved (press a command or enter to continue)";
                    return true; // Remove the motion
                }

                exit();
                return true;
            case "qa!":
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

    private void deleteLines(ArrayList<Integer> lines) {
        // Delete every unique line
        for (int i = lines.size() - 1; i >= 0; i--) {
            content.remove((int) lines.get(i));
            // If this line is above or at the cursor, move the cursor up one
            if (lines.get(i) < cursor.y
                    || (lines.get(i) == cursor.y && cursor.y == content.size()))
                cursor.up();
        }
        // If cursor is to far to the right of the its current line, move it to the end
        // of the line
        if (content.size() == 0)
            content.add("");
        if (cursor.x > content.get(cursor.y).length())
            cursor.x = content.get(cursor.y).length();
    }

    private void deleteCharacters(List<PVector> selectedCharacters) {
        // Delete each character, move cursor to beginning of selection
        for (int i = selectedCharacters.size() - 1; i >= 0; i--) {
            PVector selectedCharacter = selectedCharacters.get(i);
            String line = content.get((int) selectedCharacter.y);
            line = line.substring(0, (int) selectedCharacter.x)
                    + line.substring((int) min(selectedCharacter.x, line.length() - 1) + 1);

            if (line.length() > 0)
                content.set((int) selectedCharacter.y, line);
            else
                content.remove((int) selectedCharacter.y);
        }

        // Get left most endpoint
        PVector start = visualEndpoints.get(0);
        PVector end = visualEndpoints.get(1);
        if (start.y > end.y || (start.y == end.y && start.x > end.x))
            start = end;

        cursor.x = (int) start.x;
        cursor.y = (int) start.y;
    }

    private boolean runMotion(char motion) {
        if (isMotionGeneric(motion)) {
            switch (motion) {
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
                case '.':
                    this.motion = previousMotion;
                    parseMotion();
                    this.motion = "";
                    return true;
                case 'u':
                    undo();
                    return true;
                case default:
                    return false;
            }
        }

        if (mode == Mode.NORMAL) {
            switch (motion) {
                case 'i':
                    mode = Mode.INSERT;
                    return true;
                case 'a':
                    mode = Mode.INSERT;
                    cursor.right();
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
                case 's':
                    cursor.deleteCurrentCharacter();
                    mode = Mode.INSERT;
                    fileSaved = false;
                    return true;
                case 'p':
                    cursor.pasteAfter();
                    fileSaved = false;
                    return true;
                case 'P':
                    cursor.pasteBefore();
                    fileSaved = false;
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

        // Visual mode
        ArrayList<PVector> selectedCharacters = getSelectedCharacters();
        ArrayList<Integer> uniqueLines = getSelectedCharactersLines();
        boolean changed = false;

        switch (motion) {
            case 'C':
                deleteLines(uniqueLines);
                mode = Mode.INSERT;
                visualEndpoints.clear();
                changed = true;
                break;
            case 'c':
                deleteCharacters(selectedCharacters);
                mode = Mode.INSERT;
                visualEndpoints.clear();
                changed = true;
                break;
            case 'D':
                deleteLines(uniqueLines);
                changed = true;
                break;
            case 'd':
                deleteCharacters(selectedCharacters);
                changed = true;
                break;
            case 's':
                deleteCharacters(selectedCharacters);
                mode = Mode.INSERT;
                changed = true;
                break;
            case 'x':
                deleteCharacters(selectedCharacters);
                changed = true;
                break;
            case 'p':
                // Delete every unique line
                for (int i = uniqueLines.size() - 1; i >= 0; i--) {
                    content.remove((int) uniqueLines.get(i));
                    // If this line is above or at the cursor, move the cursor up one
                    if (uniqueLines.get(i) <= cursor.y)
                        cursor.up();
                }

                cursor.pasteAfter();
                changed = true;
                break;
            case 'P':
                // Delete every unique line
                for (int i = uniqueLines.size() - 1; i >= 0; i--) {
                    content.remove((int) uniqueLines.get(i));
                    // If this line is above or at the cursor, move the cursor up one
                    if (uniqueLines.get(i) <= cursor.y)
                        cursor.up();
                }

                cursor.pasteAfter();
                changed = true;
                break;
            case 'o':
                if (cursor.toPVector().equals(visualEndpoints.get(0))) {
                    cursor.x = (int) visualEndpoints.get(1).x;
                    cursor.y = (int) visualEndpoints.get(1).y;
                } else {
                    cursor.x = (int) visualEndpoints.get(0).x;
                    cursor.y = (int) visualEndpoints.get(0).y;
                }
                visualSelectionIndex = visualSelectionIndex == 0 ? 1 : 0;
                return true;
            case 'O':
                if (cursor.toPVector().equals(visualEndpoints.get(0))) {
                    cursor.x = (int) visualEndpoints.get(1).x;
                    cursor.y = (int) visualEndpoints.get(1).y;
                } else {
                    cursor.x = (int) visualEndpoints.get(0).x;
                    cursor.y = (int) visualEndpoints.get(0).y;
                }
                visualSelectionIndex = visualSelectionIndex == 0 ? 1 : 0;
                return true;
        }
        if (changed) {
            if (mode == Mode.VISUAL) {
                mode = Mode.NORMAL;
                visualEndpoints.clear();
            }
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
        // TODO implement the rest of these
        if (isOperatorGeneric(operator)) {
            switch (operator) {
                case 'f':
                    char searchChar = motion;
                    if (numTimes != 1)
                        searchChar = (char) ((char) numTimes + '0'); // apparently adding two chars returns an int, nice

                    if (cursor.x == content.get(cursor.y).length() - 1)
                        return true; // Return true to clear the motion
                    String contentToSearch = content.get(cursor.y).substring(cursor.x + 1);
                    int index = contentToSearch.indexOf(searchChar);
                    if (index == -1)
                        return true;

                    cursor.x += index + 1;
                    return true;
                case 'F':
                    searchChar = motion;
                    if (numTimes != 1)
                        searchChar = (char) ((char) numTimes + '0');

                    if (cursor.x == 0)
                        return true;
                    contentToSearch = content.get(cursor.y).substring(0, cursor.x);
                    index = contentToSearch.lastIndexOf(searchChar);
                    if (index == -1)
                        return true;

                    cursor.x = index;
                    return true;
                case default:
                    return false;
            }
        }

        if (mode == Mode.VISUAL) {
            switch (operator) {
                case 'y':
                    ArrayList<Integer> lines = getSelectedCharactersLines();
                    String[] linesArray = new String[lines.size()];
                    PVector[] endpoints = getSortedVisualEndpoints();
                    PVector start = endpoints[0];
                    PVector end = endpoints[1];

                    for (int i = 0; i < lines.size(); i++) {
                        String line = content.get(lines.get(i));
                        // Cut line off at end points
                        if (lines.get(i) == start.y) {
                            line = line.substring((int) start.x);
                        }
                        if (lines.get(i) == end.y) {
                            int endX = (int) end.x;
                            if (start.y == end.y)
                                endX -= (int) start.x;
                            line = line.substring(0, endX + 1);
                        }

                        linesArray[i] = line + "\n";
                    }
                    String text = String.join("", linesArray);
                    copyToClipboard(text);
                    mode = Mode.NORMAL;
                    return true;
                case 'i':
                case 'a':
                case 'r':
                    ArrayList<PVector> selectedCharacters = getSelectedCharacters();
                    char searchChar = motion;
                    if (numTimes != 1)
                        searchChar = (char) ((char) numTimes + '0');

                    for (PVector c : selectedCharacters) {
                        String line = content.get((int) c.y);
                        line = line.substring(0, (int) c.x) + searchChar + line.substring((int) c.x + 1);
                        content.set((int) c.y, line);
                    }
                    return true;
            }
            return false;
        }

        // Normal mode
        switch (operator) {
            case 'c':
            case 'd':
            case 'y':
            case 'r':
                char searchChar = motion;
                if (numTimes != 1)
                    searchChar = (char) ((char) numTimes + '0');

                if (!cursor.onCharacter())
                    return true;

                String line = content.get(cursor.y);
                line = line.substring(0, cursor.x) + searchChar + line.substring(cursor.x + 1);
                content.set(cursor.y, line);
                return true;
            case default:
                return false;
        }
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
        // Handle case where motion is just 0
        if (number == 0 && motion.length() == 0)
            motion = this.motion;
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

        int number2 = 0;
        while (motion.length() > 0 && Character.isDigit(motion.charAt(0))) {
            number2 = number2 * 10 + parseInt(motion.substring(0, 1));
            motion = motion.substring(1);
        }
        if (number2 == 0)
            number2 = 1;

        if ((Character.toLowerCase(operator) == 'f' || Character.toLowerCase(operator) == 'r')
                && this.motion.length() > 1) {
            char charToFind = ' ';
            if (motion.length() > 0)
                charToFind = motion.charAt(0);

            if (runMotion(number, operator, number2, charToFind)) {
                this.motion = "";
                return;
            }
        }
        if (operator == 'y' && this.motion.length() == 1 && mode == Mode.VISUAL) {
            runMotion(number, operator, number2, 'y');
            this.motion = "";
            return;
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
        if (keyString.equals("Enter")) {
            if (errorMessage.length() > 0) {
                errorMessage = "";
                return true;
            }
            if (motion.length() > 0) {
                if (isCommand(motion.charAt(0))) {
                    if (parseCommand())
                        motion = "";
                    return true;
                }
            } else {
                if (cursor.isOnLink()) {
                    String link = cursor.getWordWithPunctuation();
                    openInBrowser(link);
                } else {
                    cursor.down();
                }
                return true;
            }
        }

        String[] keysToIgnore = { "Shift", "Tab", "Backspace", "Delete", "Control", "Alt", "Caps Lock" };
        for (String key : keysToIgnore) {
            if (keyString.equals(key))
                return false;
        }

        motion += key;
        if (errorMessage.length() > 0)
            errorMessage = "";
        String initialMotion = String.valueOf(motion);

        parseMotion();

        if (motion.length() == 0 && !initialMotion.equals("."))
            previousMotion = initialMotion;
        return true;
    }

    private void handleControlKey() {
        if (keysPressed.size() != 2)
            return;

        switch (keyString) {
            case "A":
                mode = Mode.VISUAL;
                visualEndpoints.clear();
                visualEndpoints.add(new PVector(0, 0));
                cursor.y = content.size() - 1;
                cursor.x = cursor.getEndOfLine();
                visualEndpoints.add(cursor.toPVector());
                break;
            case "Equals":
                fontSize *= 1.1;
                textSize(fontSize);
                lineHeight = textAscent() + textDescent();
                bottomMargin = lineHeight * 2;
                break;
            case "Minus":
                fontSize /= 1.1;
                textSize(fontSize);
                lineHeight = textAscent() + textDescent();
                bottomMargin = lineHeight * 2;
                break;
            case "R":
                redo();
                break;
            case "Backspace":
                // TODO implement this
                break;
        }
    }

    private boolean handleNormalMode() {
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

    private boolean handleVisualMode() {
        // Escape and they aren't typing a motion right now
        if (keyString.equals("Escape") && motion.length() == 0) {
            mode = Mode.NORMAL;
            visualEndpoints.clear();
            return true;
        }

        return handleMotions();
    }

    public void keyPressed() {
        ArrayList<String> previousContent = new ArrayList<>(content);
        Mode previousMode = mode;

        if (keysPressed.contains("Ctrl")) {
            handleControlKey();
            return;
        }

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

        if (!content.equals(previousContent) && previousMode != Mode.INSERT && key != 'u') {
            pushToHistory();
        }
    }

    // TODO - this is quite a hacky way to do this
    public void mimicKeyPress(char key) {
        char previousKey = PComponent.key;
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
        String filePath = "[No Name]";
        if (file != null) {
            filePath = file.getAbsolutePath();

        }
        if (!fileSaved)
            filePath += " [+]";

        fill(backgroundColor);
        rectMode(CORNER);
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

        textAlign(TextAlignment.RIGHT);
        text(percentage, width - textWidth(percentage) / 2, 0);
        textAlign(TextAlignment.LEFT);

        translate(0, lineHeight);
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

    private PVector[] getSortedVisualEndpoints() {
        PVector start = visualEndpoints.get(0).copy();
        PVector end = visualEndpoints.get(1).copy();
        // Swap start and end if they're in the wrong order
        if (start.y > end.y || (start.y == end.y && start.x > end.x)) {
            PVector temp = start;
            start = end;
            end = temp;
        }

        return new PVector[] { start, end };
    }

    private ArrayList<PVector> getSelectedCharacters() {
        if (mode != Mode.VISUAL)
            return new ArrayList<PVector>();

        ArrayList<PVector> selectedCharacters = new ArrayList<>();
        PVector[] endpoints = getSortedVisualEndpoints();
        PVector start = endpoints[0];
        PVector end = endpoints[1];

        Cursor pointer = new Cursor(this, (int) start.x, (int) start.y);
        while (pointer.x != end.x || pointer.y != end.y) {
            selectedCharacters.add(pointer.toPVector());
            pointer.right();
        }
        selectedCharacters.add(pointer.toPVector());

        return selectedCharacters;
    }

    private ArrayList<Integer> getSelectedCharactersLines() {
        ArrayList<Integer> uniqueLines = new ArrayList<>();
        for (PVector selectedCharacter : getSelectedCharacters()) {
            if (!uniqueLines.contains((int) selectedCharacter.y))
                uniqueLines.add((int) selectedCharacter.y);
        }
        return uniqueLines;
    }

    private float drawSequence(String sequence, float x, float y) {
        String[] words = sequence.split(" ");
        ArrayList<String> sequences = new ArrayList<>();
        String currentSequence = "";
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (isValidURL(word)) {
                sequences.add(currentSequence);
                if (i != words.length - 1)
                    sequences.add(word + " ");
                else
                    sequences.add(word);
                currentSequence = "";
            } else {
                currentSequence += word;
                if (i != words.length - 1)
                    currentSequence += " ";
            }
        }
        if (currentSequence.length() > 0)
            sequences.add(currentSequence);

        for (String s : sequences) {
            if (isValidURL(s)) {
                // Draw word
                fill(linkColor);
                text(s, x, y);

                // Draw underline
                stroke(linkColor);
                strokeWeight(1);
                float w = textWidth(s);
                float lineY = y + lineHeight / 2 - 2;
                line(x, lineY, x + w, lineY);
                noStroke();

                x += w;
            } else {
                fill(textColor);
                text(s, x, y);
                x += textWidth(s);
            }
        }

        return x;
    }

    private void drawContent() {
        PVector position = PVector.zero();
        position.y += lineHeight / 2;

        float spaceWidth = textWidth(" ");
        float charWidth = textWidth("A");

        // Highlight the selected characters
        ArrayList<PVector> selectedCharacters = getSelectedCharacters();
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
                // Split the line into an array, separated by tabs
                String[] sequences = line.split("\t");
                float x = position.x;
                float y = position.y;
                for (String sequence : sequences) {
                    x += drawSequence(sequence, x, y);
                    x += spaceWidth * tabSize;
                }
            } else {
                drawSequence(line, position.x, position.y);
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
        if (cursorBlinkSpeed > 0 && millis() - lastBlink > cursorBlinkSpeed && mode != Mode.VISUAL) {
            lastBlink = millis();
            cursor.toggleVisibility();
        }

        drawContent();
        drawInformationSection();
    }
}