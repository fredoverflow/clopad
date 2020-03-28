import clojure.lang.Compiler;
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
    private final String sourcePath;
    private final String sourceName;

    private final FreditorWriter freditorWriter;
    public final PrintWriter printWriter;
    private final Object threadBindingFrame;

    public Console(JTabbedPane tabs, FreditorUI output, String sourcePath, String sourceName) {
        this.tabs = tabs;
        this.output = output;
        this.sourcePath = sourcePath;
        this.sourceName = sourceName;

        freditorWriter = new FreditorWriter(output);
        printWriter = new PrintWriter(freditorWriter);
        threadBindingFrame = Var.getThreadBindingFrame();
    }

    public void run(Runnable body) {
        Var.resetThreadBindingFrame(threadBindingFrame);
        Var.pushThreadBindings(RT.map(
                Compiler.SOURCE_PATH, sourcePath,
                Compiler.SOURCE, sourceName,
                RT.OUT, printWriter,
                RT.ERR, printWriter,
                Clojure.printLength, PRINT_LENGTH,
                RT.CURRENT_NS, RT.CURRENT_NS.deref()));

        freditorWriter.beforeFirstWrite = () -> {
            output.loadFromString("");
            tabs.setSelectedComponent(output);
        };
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

    public void print(Object form, String suffix) {
        synchronized (output) {
            try {
                RT.print(form, printWriter);
                printWriter.append(suffix);
            } catch (IOException impossible) {
                impossible.printStackTrace(printWriter);
            }
        }
    }
}
