import clojure.lang.Compiler;
import clojure.lang.RT;
import clojure.lang.Var;
import freditor.FreditorUI;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Console {
    private static final int PRINT_LENGTH = 100;

    private final JTabbedPane tabs;
    private final FreditorUI output;

    private final FreditorWriter freditorWriter;
    public final PrintWriter printWriter;

    public Console(JTabbedPane tabs, FreditorUI output, String sourcePath, String sourceName) {
        this.tabs = tabs;
        this.output = output;

        freditorWriter = new FreditorWriter(output);
        printWriter = new PrintWriter(freditorWriter);

        Compiler.SOURCE_PATH.bindRoot(sourcePath);
        Compiler.SOURCE.bindRoot(sourceName);
        RT.OUT.bindRoot(printWriter);
        RT.ERR.bindRoot(printWriter);
        Clojure.printLength.bindRoot(PRINT_LENGTH);
    }

    public void run(Runnable body) {
        freditorWriter.beforeFirstWrite = () -> {
            output.loadFromString("");
            tabs.setSelectedComponent(output);
        };
        Var.pushThreadBindings(RT.map(RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        try {
            body.run();
        } catch (Throwable ex) {
            Throwable cause = ex.getCause();
            if (cause == null) {
                cause = ex;
            }
            cause.printStackTrace(printWriter);
        } finally {
            Var.popThreadBindings();
        }
    }

    public void print(Object form, String suffix) {
        StringWriter stringWriter = new StringWriter();
        try {
            RT.print(form, stringWriter);
            stringWriter.append(suffix);
        } catch (Throwable ex) {
            ex.printStackTrace(new PrintWriter(stringWriter));
        } finally {
            printWriter.append(stringWriter.toString());
        }
    }
}
