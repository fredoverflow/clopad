import clojure.lang.RT;
import clojure.lang.Var;
import freditor.FreditorUI;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Console {
    private static final int PRINT_LENGTH = 100;

    private final JTabbedPane tabs;
    private final FreditorUI output;
    public final PrintWriter printWriter;
    private final Object threadBindingFrame;

    public Console(JTabbedPane tabs, FreditorUI output) {
        this.tabs = tabs;
        this.output = output;
        printWriter = new PrintWriter(new FreditorWriter(output));
        threadBindingFrame = Var.getThreadBindingFrame();
    }

    public void run(Runnable body) {
        Var.resetThreadBindingFrame(threadBindingFrame);
        Var.pushThreadBindings(RT.map(RT.OUT, printWriter, Clojure.printLength, PRINT_LENGTH, RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        output.loadFromString("");
        tabs.setSelectedComponent(output);
        try {
            body.run();
        } catch (Throwable ex) {
            Throwable cause = ex.getCause();
            if (cause == null) {
                cause = ex;
            }
            cause.printStackTrace(printWriter);
        }
    }

    public void append(CharSequence s) {
        printWriter.append(s);
    }

    public void print(Object form) {
        try {
            RT.print(form, printWriter);
        } catch (IOException impossible) {
            impossible.printStackTrace();
        }
    }
}
