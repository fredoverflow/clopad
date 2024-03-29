import clojure.lang.Compiler;
import clojure.lang.*;
import freditor.FreditorUI;
import freditor.Fronts;
import freditor.Release;
import freditor.TabbedEditors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Modifier;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

public class MainFrame extends JFrame {
    private TabbedEditors tabbedInputs;

    private FreditorUI input() {
        return tabbedInputs.getSelectedEditor();
    }

    private NamespaceExplorer namespaceExplorer;

    private FreditorUI output;
    private HashMap<Symbol, FreditorUI_symbol> infos;
    private JTabbedPane tabs;
    private final int emptyTabsWidth;
    private JSplitPane split;

    private Console console;

    public MainFrame() {
        tabbedInputs = new TabbedEditors("clopad", Flexer.instance, ClojureIndenter.instance, freditor -> {
            FreditorUI input = new FreditorUI(freditor, 72, 25);

            input.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    switch (event.getKeyCode()) {
                        case KeyEvent.VK_F1:
                            printHelpInCurrentNamespace(input().symbolNearCursor(Flexer.SYMBOL_TAIL));
                            break;

                        case KeyEvent.VK_F5:
                            evaluateWholeProgram(selectPrintFormToWriter(event));
                            break;

                        case KeyEvent.VK_F11:
                            macroexpandFormAtCursor(isolateSelectedForm(), selectMacroexpand(event), selectPrintFormToWriter(event));
                            break;

                        case KeyEvent.VK_F12:
                            evaluateFormAtCursor(isolateSelectedForm(), selectPrintFormToWriter(event));
                            break;
                    }
                }

                private IFn selectMacroexpand(KeyEvent event) {
                    return FreditorUI.isControlRespectivelyCommandDown(event) ? Clojure.macroexpandAll : Clojure.macroexpand;
                }

                private PrintFormToWriter selectPrintFormToWriter(KeyEvent event) {
                    return event.isShiftDown() ? RT::print : Clojure.pprint::invoke;
                }
            });

            registerRightClickIn(input, event -> {
                if (event.getClickCount() == 2) {
                    evaluateFormAtCursor(isolateSelectedForm(), event.isShiftDown() ? RT::print : Clojure.pprint::invoke);
                }
            }, this::printHelpInCurrentNamespace);

            return input;
        });

        if (input().length() == 0) {
            input().load(helloWorld);
        }

        namespaceExplorer = new NamespaceExplorer(this::printHelpFromExplorer);

        output = new FreditorUI(Flexer.instance, ClojureIndenter.instance, 72, 8);

        infos = new HashMap<>();

        tabs = new JTabbedPane();
        tabs.setFont(Fronts.sansSerif);
        emptyTabsWidth = tabs.getPreferredSize().width;
        tabs.addTab("output", output);
        tabs.addTab("ns explorer", namespaceExplorer);
        printHelp(Clojure.clojure_core_ns, Clojure.sourceFn.invoke(Clojure.clojure_core_ns));
        tabs.setSelectedIndex(2);

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, tabbedInputs.tabs);
        split.setResizeWeight(0.0);
        add(split);

        console = new Console(tabs, output, this::input);

        addListeners();
        boringStuff();
    }

    private static final Pattern NOT_NEWLINE = Pattern.compile("[^\n]");

    private String isolateSelectedForm() {
        String text = input().getText();
        if (input().selectionIsEmpty()) return text;

        // For simplicity, assume first form is the namespace form
        String namespaceForm = Clojure.firstForm(text);
        int namespaceEnd = namespaceForm.length();
        int selectionStart = input().selectionStart();
        if (namespaceEnd <= selectionStart) {
            String betweenNamespaceAndSelection = text.substring(namespaceEnd, selectionStart);
            String selection = text.substring(selectionStart, input().selectionEnd());
            // The whitespace preserves absolute positions for better error messages
            String whitespace = NOT_NEWLINE.matcher(betweenNamespaceAndSelection).replaceAll(" ");
            return namespaceForm + whitespace + selection;
        } else {
            return namespaceForm;
        }
    }

    private void addListeners() {
        registerRightClickIn(output, this::printHelpInCurrentNamespace);

        tabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON3) {
                    Component selectedComponent = tabs.getSelectedComponent();
                    if (!(selectedComponent instanceof FreditorUI_symbol)) {
                        tabs.removeAll();
                        tabs.addTab("output", output);
                        tabs.addTab("ns explorer", namespaceExplorer);
                        tabs.setSelectedComponent(selectedComponent);
                        infos.clear();
                    } else {
                        FreditorUI_symbol selectedSource = (FreditorUI_symbol) selectedComponent;
                        tabs.remove(selectedSource);
                        infos.remove(selectedSource.symbol);
                    }
                    input().requestFocusInWindow();
                }
            }
        });

        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            EventQueue.invokeLater(() -> {
                final int frontWidth = Fronts.front.width;
                int rest = (tabs.getWidth() - emptyTabsWidth) % frontWidth;
                if (rest > 0) {
                    if ((int) event.getNewValue() > (int) event.getOldValue()) {
                        rest -= frontWidth;
                    }
                    split.setDividerLocation(split.getDividerLocation() - rest);
                }
            });
        });
    }

    private void registerRightClickIn(FreditorUI editor, Consumer<MouseEvent> afterSelect, Consumer<String> defaultConsumer) {
        editor.onRightClick2 = (lexeme, event) -> {
            switch (lexeme.charAt(0)) {
                case '(':
                case '[':
                case '{':
                    editor.setCursorTo(editor.cursor() + 1);
                    // intentional fallthrough
                case ')':
                case ']':
                case '}':
                case ' ':
                case '\n':
                case ',':
                    editor.selectContainingForm();
                    afterSelect.accept(event);
                    break;

                default:
                    defaultConsumer.accept(lexeme);
            }
        };
    }

    private void registerRightClickIn(FreditorUI editor, Consumer<String> defaultConsumer) {
        registerRightClickIn(editor, event -> {
        }, defaultConsumer);
    }

    private void printHelpInCurrentNamespace(String lexeme) {
        console.run(false, () -> {
            Pattern userLocation = Pattern.compile(".*(?:\\Q" + input().getFile().getFileName() + "\\E)?:(\\d+)(?::(\\d+))?");
            Matcher matcher = userLocation.matcher(lexeme);
            if (matcher.matches()) {
                int line = Integer.parseInt(matcher.group(1));
                int column = Optional.ofNullable(matcher.group(2)).map(Integer::parseInt).orElse(1);
                input().setCursorTo(line - 1, column - 1);
                EventQueue.invokeLater(input()::requestFocusInWindow);
            } else {
                evaluateNamespaceFormsBeforeCursor(input().getText(), formAtCursor -> {
                    Namespace namespace = (Namespace) RT.CURRENT_NS.deref();
                    printPotentiallySpecialHelp(namespace, Symbol.create(lexeme));
                });
            }
        });
    }

    private void printHelpFromHelp(String shrunkLexeme) {
        String lexeme = Java.expandClojureLangPackage(shrunkLexeme);
        console.run(false, () -> {
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

    public void printHelpFromExplorer(Namespace namespace, Symbol symbol) {
        console.run(false, () -> printHelp(namespace, symbol));
        input().requestFocusInWindow();
    }

    private void printHelp(Namespace namespace, Symbol symbol) {
        String name = symbol.toString();
        if (name.endsWith(".") && !name.startsWith(".")) {
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
        String header = Java.classChain(clazz) + Java.allInterfaces(clazz, output.visibleColumns());
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
        info.load(help.toString());
        tabs.setSelectedComponent(info);
    }

    private FreditorUI_symbol newInfo(Symbol symbol) {
        FreditorUI_symbol info = new FreditorUI_symbol(Flexer.instance, ClojureIndenter.instance, 72, 8, symbol);
        registerRightClickIn(info, this::printHelpFromHelp);
        tabs.addTab(symbol.getName(), info);
        return info;
    }

    private void macroexpandFormAtCursor(String text, IFn macroexpand, PrintFormToWriter printFormToWriter) {
        console.run(true, () -> {
            evaluateNamespaceFormsBeforeCursor(text, formAtCursor -> {
                console.print(printFormToWriter, "", formAtCursor, "¤\n");
                Object expansion = macroexpand.invoke(formAtCursor);
                printResultValueAndType(printFormToWriter, expansion);
            });
        });
    }

    private void evaluateWholeProgram(PrintFormToWriter printFormToWriter) {
        console.run(true, () -> {
            output.load("");
            Clojure.loadFromScratch(input().getText(), input().getFile(), result -> {
                namespaceExplorer.updateNamespaces();
                printResultValueAndType(printFormToWriter, result);
            });
        });
    }

    private void printResultValueAndType(PrintFormToWriter printFormToWriter, Object result) {
        if (result == null) {
            console.print(printFormToWriter, "", null, "\n" + timestamp() + "\n\n");
        } else {
            console.print(printFormToWriter, "", result, "\n" + result.getClass().getTypeName() + "\n" + timestamp() + "\n\n");
        }
    }

    private static final DateTimeFormatter _HH_mm_ss_SSS = DateTimeFormatter.ofPattern(";HH:mm:ss.SSS");

    private static String timestamp() {
        return LocalTime.now().format(_HH_mm_ss_SSS);
    }

    private void evaluateFormAtCursor(String text, PrintFormToWriter printFormToWriter) {
        console.run(true, () -> {
            evaluateNamespaceFormsBeforeCursor(text, formAtCursor -> {
                console.print(printFormToWriter, "", formAtCursor, "\n");
                Object result = Clojure.isNamespaceForm(formAtCursor) ? null : Compiler.eval(formAtCursor, false);
                printResultValueAndType(printFormToWriter, result);
            });
        });
    }

    private void evaluateNamespaceFormsBeforeCursor(String text, Consumer<Object> formContinuation) {
        Clojure.evaluateNamespaceFormsBefore(text, input().getFile(),
                input().row() + 1, input().column() + 1, namespaceExplorer::updateNamespaces, formContinuation);
    }

    private void boringStuff() {
        setTitle("clopad version " + Release.compilationDate(MainFrame.class) + " @ " + input().getFile().getParent());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        tabbedInputs.saveOnExit(this);
        pack();
        setVisible(true);
        input().requestFocusInWindow();
    }

    private static final String helloWorld = ""
            + ";; F1  show source or doc-string (same as right-click)\n"
            + ";; F5  evaluate    whole program\n"
            + ";; F11 macroexpand top-level or selected form (CTRL also expands nested macros)\n"
            + ";; F12 evaluate    top-level or selected form\n"
            + ";; SHIFT with F5/F11/F12 disables pretty-printing\n\n"
            + "(ns user\n  (:require [clojure.string :as string]))\n\n"
            + "(defn square [x]\n  (* x x))\n\n"
            + "(->> (range 1 11)\n  (map square )\n  (string/join \", \" ))\n";
}
