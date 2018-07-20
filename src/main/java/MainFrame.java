import clojure.lang.Compiler;
import clojure.lang.*;
import freditor.FreditorUI;
import freditor.LineNumbers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Comparator;

public class MainFrame extends JFrame {
    private static final int PRINT_LENGTH = 100;
    private static final Object EOF = new Object();

    private Editor input;
    private FreditorUI output;
    private FreditorUI source;
    private JTabbedPane tabs;

    private JComboBox<Namespace> namespaces;
    private JList<Symbol> names;
    private JTextField filter;

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

        namespaces = new JComboBox<>();
        names = new JList<>();
        names.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        filter = new JTextField();

        JPanel namespaceExplorer = new JPanel(new BorderLayout());
        namespaceExplorer.add(namespaces, BorderLayout.NORTH);
        namespaceExplorer.add(new JScrollPane(names), BorderLayout.CENTER);
        namespaceExplorer.add(filter, BorderLayout.SOUTH);

        JPanel up = new JPanel(new BorderLayout());
        up.add(inputWithLineNumbers, BorderLayout.CENTER);
        up.add(namespaceExplorer, BorderLayout.EAST);

        tabs = new JTabbedPane();
        tabs.addTab("output", output);
        tabs.addTab("source", source);

        add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, up, tabs));

        addListeners();
        boringStuff();
    }

    private void updateNamespaces() {
        ISeq allNamespaces = Namespace.all();
        if (RT.count(allNamespaces) != namespaces.getItemCount()) {
            namespaces.removeAllItems();
            ISeqSpliterator.<Namespace>stream(allNamespaces)
                    .sorted(Comparator.comparing(Namespace::toString))
                    .forEach(namespaces::addItem);
        }
    }

    private void filterSymbols() {
        Namespace namespace = (Namespace) namespaces.getSelectedItem();
        if (namespace == null) return;

        ISeq interns = RT.keys(Clojure.nsInterns.invoke(namespace.name));
        Symbol[] symbols = ISeqSpliterator.<Symbol>stream(interns)
                .filter(symbol -> symbol.getName().contains(filter.getText()))
                .sorted(Comparator.comparing(Symbol::getName))
                .toArray(Symbol[]::new);
        names.setListData(symbols);

        filter.setBackground(symbols.length > 0 || filter.getText().isEmpty() ? Color.WHITE : Color.RED);
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

        namespaces.addItemListener(event -> filterSymbols());
        updateNamespaces();
        namespaces.setSelectedIndex(0);

        names.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) return;

            Object namespace = namespaces.getSelectedItem();
            if (namespace == null) return;

            Symbol unqualified = names.getSelectedValue();
            if (unqualified == null) return;

            Symbol qualified = Symbol.create(namespace.toString(), unqualified.getName());
            printSource(qualified);
        });

        filter.getDocument().addDocumentListener(new DocumentAdapter(event -> filterSymbols()));
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
            if (isNamespaceForm(form)) {
                updateNamespaces();
            }
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

            if (isNamespaceForm(form)) {
                Compiler.eval(form, false);
            }
            form = result;
        }
        return form;
    }

    private boolean isNamespaceForm(Object form) {
        return form instanceof PersistentList && ((PersistentList) form).first().equals(Clojure.ns);
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
            updateNamespaces();
        }
    }

    private void printSource(Object symbolOrLexeme) {
        StringWriter console = new StringWriter();
        Var.pushThreadBindings(RT.map(RT.OUT, console, Clojure.printLength, PRINT_LENGTH, RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        try {
            evaluateNamespaceFormsBeforeCursor();

            Object symbol = Clojure.symbol.invoke(symbolOrLexeme);
            Object source = Clojure.sourceFn.invoke(symbol);
            if (source != null) {
                console.append(source.toString());
            }
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
