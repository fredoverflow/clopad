import clojure.lang.Compiler;
import clojure.lang.*;
import freditor.FreditorUI;
import freditor.LineNumbers;

import javax.swing.*;
import java.awt.event.*;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

public class MainFrame extends JFrame {
    private static final int PRINT_LENGTH = 100;
    private static final Object EOF = new Object();

    private Editor input;
    private FreditorUI output;
    private FreditorUI source;
    private JTabbedPane tabs;

    public MainFrame() {
        super(Editor.filename);

        input = new Editor();
        output = new FreditorUI(OutputFlexer.instance, ClojureIndenter.instance, 80, 10);
        source = new FreditorUI(Flexer.instance, ClojureIndenter.instance, 80, 10);

        JPanel inputWithLineNumbers = new JPanel();
        inputWithLineNumbers.setLayout(new BoxLayout(inputWithLineNumbers, BoxLayout.X_AXIS));
        inputWithLineNumbers.add(new LineNumbers(input));
        inputWithLineNumbers.add(input);
        input.setComponentToRepaint(inputWithLineNumbers);

        tabs = new JTabbedPane();
        tabs.addTab("output", output);
        tabs.addTab("source", source);

        add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputWithLineNumbers, tabs));

        addListeners();
        boringStuff();
    }

    private void addListeners() {
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_F1:
                        printSource(input.lexemeAtCursor());
                        break;
                    case KeyEvent.VK_F5:
                        evaluateWholeProgram();
                        break;
                    case KeyEvent.VK_F12:
                        evaluateFormAtCursor();
                        break;
                }
            }
        });
        input.onRightClick = this::printSource;
        source.onRightClick = this::printSource;
    }

    private void evaluateFormAtCursor() {
        input.tryToSaveCode();
        StringWriter console = new StringWriter();
        Var.pushThreadBindings(RT.map(RT.OUT, console, Clojure.printLength, PRINT_LENGTH, RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        try {
            Object form = evaluateNamespaceFormsBeforeCursor();
            RT.print(form, console);
            console.append("\n\n");
            Object result = Compiler.eval(form, false);
            RT.print(result, console);
        } catch (Exception ex) {
            appendCause(console, ex);
        } finally {
            Var.popThreadBindings();
            output.loadFromString(console.toString());
            tabs.setSelectedComponent(output);
        }
    }

    private Object evaluateNamespaceFormsBeforeCursor() {
        String text = input.getText();
        Reader reader = new StringReader(text);
        LineNumberingPushbackReader rdr = new LineNumberingPushbackReader(reader);
        Object form = null;

        int row = 1 + input.row();
        int column = 1 + input.column();
        int line;
        while ((line = rdr.getLineNumber()) < row || line == row && rdr.getColumnNumber() < column) {
            Object result = LispReader.read(rdr, false, EOF, false, null);
            if (result == EOF) break;

            evaluateNamespace(form);
            form = result;
        }
        return form;
    }

    private void evaluateNamespace(Object form) {
        if (form instanceof PersistentList) {
            PersistentList list = (PersistentList) form;
            if (list.first().equals(Clojure.ns)) {
                Compiler.eval(form, false);
            }
        }
    }

    private void appendCause(StringWriter console, Exception ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            cause = ex;
        }
        console.append(cause.getMessage());
    }

    private void evaluateWholeProgram() {
        input.tryToSaveCode();
        StringWriter console = new StringWriter();
        Var.pushThreadBindings(RT.map(RT.OUT, console, Clojure.printLength, PRINT_LENGTH));
        try {
            input.requestFocusInWindow();
            String text = input.getText();
            Reader reader = new StringReader(text);
            Object result = Compiler.load(reader, Editor.directory, "clopad.txt");
            RT.print(result, console);
        } catch (Compiler.CompilerException ex) {
            console.append(ex.getCause().getMessage());
            if (ex.line > 0) {
                String message = ex.getMessage();
                int colon = message.lastIndexOf(':');
                int paren = message.lastIndexOf(')');
                int column = Integer.parseInt(message.substring(colon + 1, paren));
                input.setCursorTo(ex.line - 1, column - 1);
            }
        } catch (Exception ex) {
            appendCause(console, ex);
        } finally {
            Var.popThreadBindings();
            output.loadFromString(console.toString());
            tabs.setSelectedComponent(output);
        }
    }

    private void printSource(String lexeme) {
        StringWriter console = new StringWriter();
        Var.pushThreadBindings(RT.map(RT.OUT, console, Clojure.printLength, PRINT_LENGTH, RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        try {
            evaluateNamespaceFormsBeforeCursor();

            Object symbol = Clojure.symbol.invoke(lexeme);
            Object source = Clojure.sourceFn.invoke(symbol);
            console.append(String.valueOf(source));
        } catch (LispReader.ReaderException ex) {
            console.append(ex.getCause().getMessage());
        } catch (Exception ex) {
            appendCause(console, ex);
        } finally {
            Var.popThreadBindings();
            source.loadFromString(console.toString());
            tabs.setSelectedComponent(source);
        }
    }

    private void boringStuff() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                input.tryToSaveCode();
            }
        });
        pack();
        setVisible(true);
        input.requestFocusInWindow();
    }
}
