import clojure.lang.*;
import clojure.lang.Compiler;
import freditor.FreditorUI;
import freditor.LineNumbers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

public class MainFrame extends JFrame {
    private Editor input;
    private JButton button;
    private FreditorUI output;

    public MainFrame() {
        super(Editor.filename);

        input = new Editor();
        button = new JButton("evaluate");
        output = new FreditorUI(OutputFlexer.instance, ClojureIndenter.instance, 80, 10);

        JPanel inputWithLineNumbers = new JPanel();
        inputWithLineNumbers.setLayout(new BoxLayout(inputWithLineNumbers, BoxLayout.X_AXIS));
        inputWithLineNumbers.add(new LineNumbers(input));
        inputWithLineNumbers.add(input);
        input.setComponentToRepaint(inputWithLineNumbers);

        add(inputWithLineNumbers, BorderLayout.CENTER);
        JPanel down = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel();
        buttons.add(button);
        down.add(buttons, BorderLayout.NORTH);
        down.add(output, BorderLayout.CENTER);
        add(down, BorderLayout.SOUTH);

        button.addActionListener(this::evaluate);
        boringStuff();
    }

    private void evaluate(ActionEvent event) {
        input.tryToSaveCode();
        StringWriter console = new StringWriter();
        Var.pushThreadBindings(RT.map(RT.OUT, console));
        try {
            input.requestFocusInWindow();
            String text = input.getText();
            Reader reader = new StringReader(text);
            Object result = Compiler.load(reader, Editor.directory, "clopad.txt");
            if (result instanceof IPending) {
                final int PENDING_LIMIT = 100;
                IFn take = Var.find(Symbol.create("clojure.core", "take"));
                result = take.invoke(PENDING_LIMIT, result);
            }
            RT.print(result, console);
        } catch (Compiler.CompilerException ex) {
            console.append("" + ex.line).append(": ").append(ex.getCause().getMessage());
        } catch (IOException impossible) {
            throw new RuntimeException(impossible);
        } finally {
            Var.popThreadBindings();
            output.loadFromString(console.toString());
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
