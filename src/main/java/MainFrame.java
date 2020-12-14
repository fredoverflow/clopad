import clojure.lang.Compiler;
import clojure.lang.*;
import freditor.FreditorUI;
import freditor.Front;
import freditor.LineNumbers;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.Consumer;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

public class MainFrame extends JFrame {
    private Editor input;
    private FreditorUI output;
    private HashMap<Symbol, FreditorUI_symbol> infos;
    private JTabbedPane tabs;
    private JSplitPane split;

    private JComboBox<Namespace> namespaces;
    private JList<Symbol> names;
    private JTextField filter;

    private Console console;

    public MainFrame() {
        input = new Editor();
        output = new FreditorUI(OutputFlexer.instance, ClojureIndenter.instance, 80, 8);
        infos = new HashMap<>();

        setTitle(input.autosaver.pathname);

        JPanel inputWithLineNumbers = new JPanel();
        inputWithLineNumbers.setLayout(new BoxLayout(inputWithLineNumbers, BoxLayout.X_AXIS));
        inputWithLineNumbers.add(new LineNumbers(input));
        inputWithLineNumbers.add(input);
        input.setComponentToRepaint(inputWithLineNumbers);

        namespaces = new JComboBox<>();
        namespaces.setFont(Front.sansSerif);

        names = new JList<>();
        names.setFont(Front.sansSerif);
        names.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        filter = new JTextField();
        filter.setFont(Front.sansSerif);

        JPanel namespaceExplorer = new JPanel(new BorderLayout());
        namespaceExplorer.add(namespaces, BorderLayout.NORTH);
        namespaceExplorer.add(new JScrollPane(names), BorderLayout.CENTER);
        namespaceExplorer.add(filter, BorderLayout.SOUTH);

        JPanel up = new JPanel(new BorderLayout());
        up.add(inputWithLineNumbers, BorderLayout.CENTER);
        up.add(namespaceExplorer, BorderLayout.EAST);

        filter.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (filter.getText().isEmpty()) {
                        up.remove(namespaceExplorer);
                        up.revalidate();
                    }
                    input.requestFocusInWindow();
                }
            }
        });

        tabs = new JTabbedPane();
        tabs.addTab("output", output);

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, up, tabs);
        split.setResizeWeight(1.0);
        add(split);

        console = new Console(tabs, output, input, input.autosaver.pathname, input.autosaver.filename);
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
                        printHelpInCurrentNamespace(input.symbolNearCursor(Flexer.SYMBOL_TAIL));
                        break;

                    case KeyEvent.VK_F5:
                        evaluateWholeProgram(selectPrintFormToWriter(event));
                        break;

                    case KeyEvent.VK_F11:
                        macroexpandFormAtCursor(selectMacroexpand(event), selectPrintFormToWriter(event));
                        break;

                    case KeyEvent.VK_F12:
                        evaluateFormAtCursor(selectPrintFormToWriter(event));
                        break;
                }
            }

            private IFn selectMacroexpand(KeyEvent event) {
                return Editor.isControlRespectivelyCommandDown(event) ? Clojure.macroexpandAll : Clojure.macroexpand;
            }

            private PrintFormToWriter selectPrintFormToWriter(KeyEvent event) {
                return event.isShiftDown() ? Clojure.pprint::invoke : RT::print;
            }
        });

        input.onRightClick = this::printHelpInCurrentNamespace;
        output.onRightClick = this::printHelpInCurrentNamespace;

        tabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) {
                    Component selectedComponent = tabs.getSelectedComponent();
                    if (selectedComponent == output) {
                        tabs.removeAll();
                        tabs.addTab("output", output);
                        infos.clear();
                    } else {
                        FreditorUI_symbol selectedSource = (FreditorUI_symbol) selectedComponent;
                        tabs.remove(selectedSource);
                        infos.remove(selectedSource.symbol);
                    }
                    input.requestFocusInWindow();
                }
            }
        });

        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            SwingUtilities.invokeLater(() -> {
                final int frontHeight = Front.front.height;
                int rest = tabs.getSelectedComponent().getHeight() % frontHeight;
                if (rest > 0) {
                    if ((int) event.getNewValue() < (int) event.getOldValue()) {
                        rest -= frontHeight;
                    }
                    split.setDividerLocation(split.getDividerLocation() + rest);
                }
            });
        });

        namespaces.addItemListener(event -> filterSymbols());
        updateNamespaces();
        names.addListSelectionListener(this::printHelpFromExplorer);

        filter.getDocument().addDocumentListener(new DocumentAdapter(event -> filterSymbols()));
    }

    private void printHelpInCurrentNamespace(String lexeme) {
        console.run(() -> {
            evaluateNamespaceFormsBeforeCursor(formAtCursor -> {
                Namespace namespace = (Namespace) RT.CURRENT_NS.deref();
                printPotentiallySpecialHelp(namespace, Symbol.create(lexeme));
            });
        });
    }

    private void printHelpFromHelp(String lexeme) {
        console.run(() -> {
            FreditorUI_symbol selected = (FreditorUI_symbol) tabs.getSelectedComponent();
            String selectedSymbolNamespace = selected.symbol.getNamespace();
            if (selectedSymbolNamespace != null) {
                Namespace namespace = Namespace.find(Symbol.create(selectedSymbolNamespace));
                printPotentiallySpecialHelp(namespace, Symbol.create(lexeme));
            } else {
                printHelpInCurrentNamespace(lexeme);
            }
        });
    }

    private void printPotentiallySpecialHelp(Namespace namespace, Symbol symbol) {
        String specialHelp = SpecialForm.help(symbol.toString());
        if (specialHelp != null) {
            printHelp(symbol, specialHelp);
        } else {
            printHelp(namespace, symbol);
        }
    }

    private void printHelpFromExplorer(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;

        Object namespace = namespaces.getSelectedItem();
        if (namespace == null) return;

        Symbol symbol = names.getSelectedValue();
        if (symbol == null) return;

        console.run(() -> printHelp((Namespace) namespace, symbol));
        input.requestFocusInWindow();
    }

    private void printHelp(Namespace namespace, Symbol symbol) {
        String name = symbol.toString();
        if (name.endsWith(".")) {
            printHelpConstructor(namespace, Symbol.create(name.substring(0, name.length() - 1)));
        } else {
            printHelpNonConstructor(namespace, symbol);
        }
    }

    private void printHelpConstructor(Namespace namespace, Symbol symbol) {
        Object obj = Compiler.maybeResolveIn(namespace, symbol);
        if (obj == null) throw new RuntimeException("Unable to resolve symbol: " + symbol + " in this context");

        if (obj instanceof Class<?>) {
            Class<?> clazz = (Class<?>) obj;
            String constructors = Java.sortedConstructors(clazz, Modifier::isPublic, "");
            printHelp(symbol, constructors);
        } else {
            console.printWriter.println(obj);
        }
    }

    private void printHelpNonConstructor(Namespace namespace, Symbol symbol) {
        Object obj = Compiler.maybeResolveIn(namespace, symbol);
        if (obj == null) throw new RuntimeException("Unable to resolve symbol: " + symbol + " in this context");

        if (obj instanceof Var) {
            Var var = (Var) obj;
            Symbol resolved = Symbol.create(var.ns.toString(), var.sym.getName());
            printHelp(var, resolved);
        } else if (obj instanceof Class<?>) {
            Class<?> clazz = (Class<?>) obj;
            printHelpMembers(symbol, clazz);
        } else {
            console.printWriter.println(obj);
        }
    }

    private void printHelpMembers(Symbol symbol, Class<?> clazz) {
        String header = Java.classChain(clazz) + Java.allInterfaces(clazz);
        if (!header.isEmpty()) {
            header += "\n";
        }

        String methods = Java.sortedMethods(clazz, mod -> isPublic(mod) && !isStatic(mod), "\n");
        String staticFields = Java.sortedFields(clazz, mod -> isPublic(mod) && isStatic(mod), "\n");
        String staticMethods = Java.sortedMethods(clazz, mod -> isPublic(mod) && isStatic(mod), "");

        if (staticFields.isEmpty() && staticMethods.isEmpty()) {
            printHelp(symbol, header + methods);
        } else {
            printHelp(symbol, header + methods + "======== static members ========\n\n" + staticFields + staticMethods);
        }
    }

    private void printHelp(Var var, Symbol resolved) {
        Object help = Clojure.sourceFn.invoke(resolved);
        if (help == null) {
            IPersistentMap meta = var.meta();
            if (meta != null) {
                help = meta.valAt(Clojure.doc);
            }
            if (help == null) throw new RuntimeException("No source or doc found for symbol: " + resolved);
        }
        printHelp(resolved, help);
    }

    private void printHelp(Symbol resolved, Object help) {
        FreditorUI_symbol info = infos.computeIfAbsent(resolved, this::newInfo);
        info.loadFromString(help.toString());
        tabs.setSelectedComponent(info);
    }

    private FreditorUI_symbol newInfo(Symbol symbol) {
        FreditorUI_symbol info = new FreditorUI_symbol(Flexer.instance, ClojureIndenter.instance, 80, 8, symbol);
        info.onRightClick = this::printHelpFromHelp;
        tabs.addTab(symbol.getName(), info);
        return info;
    }

    private void printForm(String title, Object form, PrintFormToWriter printFormToWriter) {
        Symbol symbol = Symbol.create(RT.CURRENT_NS.deref().toString(), title);
        FreditorUI_symbol info = infos.computeIfAbsent(symbol, this::newInfo);
        StringWriter stringWriter = new StringWriter();
        try {
            printFormToWriter.print(form, stringWriter);
        } catch (IOException ex) {
            ex.printStackTrace(new PrintWriter(stringWriter));
        } finally {
            info.loadFromString(stringWriter.toString());
        }
        tabs.setSelectedComponent(info);
    }

    private void macroexpandFormAtCursor(IFn macroexpand, PrintFormToWriter printFormToWriter) {
        console.run(() -> {
            evaluateNamespaceFormsBeforeCursor(formAtCursor -> {
                Object expansion = macroexpand.invoke(formAtCursor);
                printForm("macro expansion", expansion, printFormToWriter);
            });
        });
    }

    private void evaluateWholeProgram(PrintFormToWriter printFormToWriter) {
        console.run(() -> {
            Clojure.loadFromScratch(input.getText(), input.autosaver.pathname, input.autosaver.filename, result -> {
                updateNamespaces();
                printResultValueAndType(printFormToWriter, result);
            });
        });
    }

    private void printResultValueAndType(PrintFormToWriter printFormToWriter, Object result) {
        if (result == null) {
            console.print(printFormToWriter, result);
        } else {
            console.print(printFormToWriter, result, Console.NEWLINE, result.getClass());
        }
    }

    private void evaluateFormAtCursor(PrintFormToWriter printFormToWriter) {
        console.run(() -> {
            evaluateNamespaceFormsBeforeCursor(formAtCursor -> {
                console.print(formAtCursor, Console.NEWLINE, Console.NEWLINE);
                Object result = Clojure.isNamespaceForm(formAtCursor) ? null : Compiler.eval(formAtCursor, false);
                printResultValueAndType(printFormToWriter, result);
            });
        });
    }

    private void evaluateNamespaceFormsBeforeCursor(Consumer<Object> formContinuation) {
        Clojure.evaluateNamespaceFormsBefore(input.getText(), input.autosaver.pathname, input.autosaver.filename,
                1 + input.row(), 1 + input.column(), this::updateNamespaces, formContinuation);
    }

    private void boringStuff() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                input.autosaver.save();
            }
        });
        pack();
        setVisible(true);
        input.requestFocusInWindow();
    }
}
