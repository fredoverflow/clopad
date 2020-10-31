import clojure.lang.Compiler;
import clojure.lang.RT;
import clojure.lang.Var;
import freditor.FreditorUI;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Console {
    // The redundant new String(original) is necessary here because
    // NEWLINE must be distinct from interned Clojure string literals.
    static final String NEWLINE = new String("\n");

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

    public void print(Object... forms) {
        print(RT::print, forms);
    }

    public void print(PrintFormToWriter printFormToWriter, Object... forms) {
        StringWriter stringWriter = new StringWriter();
        try {
            for (Object form : forms) {
                if (form == NEWLINE) {
                    stringWriter.append(NEWLINE);
                } else {
                    printFormToWriter.print(form, stringWriter);
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace(new PrintWriter(stringWriter));
        } finally {
            printWriter.append(stringWriter.toString());
        }
    }
}
