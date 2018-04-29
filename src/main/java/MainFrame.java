import clojure.lang.Compiler;
import clojure.lang.LazySeq;
import freditor.FreditorUI;
import freditor.LineNumbers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Reader;
import java.io.StringReader;

public class MainFrame extends JFrame {
    private Editor input;
    private JButton button;
    private FreditorUI output;

    public MainFrame() {
        super(Editor.filename);

        input = new Editor();
        button = new JButton("evaluate");
        output = new FreditorUI(new Flexer(), 80, 5);

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
        try {
            input.requestFocusInWindow();
            String text = input.getText();
            Reader reader = new StringReader(text);
            Object result = Compiler.load(reader, Editor.directory, "clopad.txt");
            if (result instanceof LazySeq) {
                result = ((LazySeq) result).seq();
            }
            String string = result.toString();
            output.loadFromString(string);
        } catch (Compiler.CompilerException ex) {
            String message = ex.getCause().getMessage();
            output.loadFromString(message);
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
