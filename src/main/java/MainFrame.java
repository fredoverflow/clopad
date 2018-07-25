import clojure.lang.Compiler;
import clojure.lang.*;
import freditor.FreditorUI;
import freditor.LineNumbers;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Comparator;
import java.util.HashMap;

public class MainFrame extends JFrame {
    private Editor input;
    private FreditorUI output;
    private HashMap<Symbol, FreditorUI_symbol> sources;
    private JTabbedPane tabs;

    private JComboBox<Namespace> namespaces;
    private JList<Symbol> names;
    private JTextField filter;

    private Console console;

    public MainFrame() {
        super(Editor.filename);

        input = new Editor();
        output = new FreditorUI(OutputFlexer.instance, ClojureIndenter.instance, 80, 10);
        sources = new HashMap<>();

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

        add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, up, tabs));

        console = new Console(tabs, output);
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
                        printSourceFromInput(input.lexemeAtCursor());
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

        input.onRightClick = this::printSourceFromInput;

        tabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) {
                    Component selectedComponent = tabs.getSelectedComponent();
                    if (selectedComponent != output) {
                        FreditorUI_symbol selectedSource = (FreditorUI_symbol) selectedComponent;
                        tabs.remove(selectedSource);
                        sources.remove(selectedSource.symbol);
                    }
                }
            }
        });

        namespaces.addItemListener(event -> filterSymbols());
        updateNamespaces();
        names.addListSelectionListener(this::printSourceFromExplorer);

        filter.getDocument().addDocumentListener(new DocumentAdapter(event -> filterSymbols()));
    }

    private void printSourceFromInput(String lexeme) {
        console.run(() -> {
            evaluateNamespaceFormsBeforeCursor();
            Namespace namespace = (Namespace) RT.CURRENT_NS.deref();
            printSource(namespace, Symbol.create(lexeme));
        });
    }

    private void printSourceFromSource(String lexeme) {
        console.run(() -> {
            FreditorUI_symbol selected = (FreditorUI_symbol) tabs.getSelectedComponent();
            Namespace namespace = Namespace.find(Symbol.create(selected.symbol.getNamespace()));
            printSource(namespace, Symbol.create(lexeme));
        });
    }

    private void printSource(Namespace namespace, Symbol symbol) {
        Var var = (Var) Compiler.maybeResolveIn(namespace, symbol);
        if (var == null) throw new RuntimeException("Unable to resolve symbol: " + symbol + " in this context");

        Symbol resolved = Symbol.create(var.ns.toString(), var.sym.getName());
        printSource(resolved);
    }

    private void printSource(Symbol resolved) {
        Object source = Clojure.sourceFn.invoke(resolved);
        if (source == null) throw new RuntimeException("No source available for symbol: " + resolved);

        console.append(source);
        console.target = sources.computeIfAbsent(resolved, symbol -> {
            FreditorUI_symbol ui = new FreditorUI_symbol(Flexer.instance, ClojureIndenter.instance, 80, 10, symbol);
            ui.onRightClick = this::printSourceFromSource;
            tabs.addTab(symbol.getName(), ui);
            return ui;
        });
    }

    private void printSourceFromExplorer(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;

        Object namespace = namespaces.getSelectedItem();
        if (namespace == null) return;

        Symbol unqualified = names.getSelectedValue();
        if (unqualified == null) return;

        console.run(() -> {
            Symbol resolved = Symbol.create(namespace.toString(), unqualified.getName());
            printSource(resolved);
        });
    }

    private void evaluateWholeProgram() {
        console.run(() -> {
            try {
                input.tryToSaveCode();
                input.requestFocusInWindow();
                String text = input.getText();
                Reader reader = new StringReader(text);
                Object result = Compiler.load(reader, Editor.directory, "clopad.txt");
                console.print(result);
                updateNamespaces();
            } catch (Compiler.CompilerException ex) {
                console.append(ex.getCause().getMessage());
                if (ex.line > 0) {
                    String message = ex.getMessage();
                    int colon = message.lastIndexOf(':');
                    int paren = message.lastIndexOf(')');
                    int column = Integer.parseInt(message.substring(colon + 1, paren));
                    input.setCursorTo(ex.line - 1, column - 1);
                }
            }
        });
    }

    private void evaluateFormAtCursor() {
        console.run(() -> {
            input.tryToSaveCode();
            Object form = evaluateNamespaceFormsBeforeCursor();
            console.print(form);
            console.append("\n\n");
            Object result = Compiler.eval(form, false);
            console.print(result);
            if (isNamespaceForm(form)) {
                updateNamespaces();
            }
        });
    }

    private Object evaluateNamespaceFormsBeforeCursor() {
        String text = input.getText();
        Reader reader = new StringReader(text);
        LineNumberingPushbackReader rdr = new LineNumberingPushbackReader(reader);

        int row = 1 + input.row();
        int column = 1 + input.column();
        for (; ; ) {
            Object form = LispReader.read(rdr, false, null, false, null);
            if (skipWhitespace(rdr) == -1) return form;

            int line = rdr.getLineNumber();
            if (line > row || line == row && rdr.getColumnNumber() > column) return form;

            if (isNamespaceForm(form)) {
                Compiler.eval(form, false);
                updateNamespaces();
            }
        }
    }

    private int skipWhitespace(PushbackReader rdr) {
        int ch = -1;
        try {
            do {
                ch = rdr.read();
            } while (Character.isWhitespace(ch) || ch == ',');
            rdr.unread(ch);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return ch;
    }

    private boolean isNamespaceForm(Object form) {
        return form instanceof PersistentList && ((PersistentList) form).first().equals(Clojure.ns);
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
