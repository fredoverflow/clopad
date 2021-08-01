import clojure.lang.Compiler;
import clojure.lang.*;
import freditor.FreditorUI;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class Console {
    public static final Raw NEWLINE = new Raw("\n");

    private static final int PRINT_LENGTH = 100;

    private final JTabbedPane tabs;
    private final FreditorUI output;
    private final Editor input;

    private final FreditorWriter freditorWriter;
    public final PrintWriter printWriter;

    public Console(JTabbedPane tabs, FreditorUI output, Editor input, String sourcePath, String sourceName) {
        this.tabs = tabs;
        this.output = output;
        this.input = input;

        freditorWriter = new FreditorWriter(output);
        printWriter = new PrintWriter(freditorWriter);

        Compiler.SOURCE_PATH.bindRoot(sourcePath);
        Compiler.SOURCE.bindRoot(sourceName);
        RT.OUT.bindRoot(printWriter);
        RT.ERR.bindRoot(printWriter);
        Clojure.printLength.bindRoot(PRINT_LENGTH);
    }

    public void run(boolean setCursor, Runnable body) {
        freditorWriter.beforeFirstWrite = () -> {
            output.loadFromString("");
            tabs.setSelectedComponent(output);
        };
        Var.pushThreadBindings(RT.map(RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        try {
            input.autosaver.save();
            body.run();
        } catch (Compiler.CompilerException ex) {
            printWriter.println();
            Throwable cause = ex.getCause();
            StackTraceElement[] stackTrace = cause.getStackTrace();
            StackTraceElement[] userElements = Arrays.stream(stackTrace)
                    .filter(element -> input.autosaver.filename.equals(element.getFileName()))
                    .toArray(StackTraceElement[]::new);
            if (userElements.length > 0) {
                printWriter.println(cause);
                for (StackTraceElement element : userElements) {
                    printWriter.println("\tat " + element);
                }
                if (setCursor) {
                    int line = userElements[0].getLineNumber();
                    input.setCursorTo(line - 1, 0);
                }
                printWriter.println();
                cause.printStackTrace(printWriter);
            } else {
                cause.printStackTrace(printWriter);
                if (setCursor && ex.line > 0) {
                    IPersistentMap data = ex.getData();
                    Integer column = (Integer) data.valAt(Compiler.CompilerException.ERR_COLUMN);
                    input.setCursorTo(ex.line - 1, column - 1);
                }
            }
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
                if (form instanceof Raw) {
                    stringWriter.append(form.toString());
                } else {
                    printFormToWriter.print(form, stringWriter);
                }
            }
        } catch (IOException ex) {
            throw Util.sneakyThrow(ex);
        } finally {
            printWriter.append(stringWriter.toString());
        }
    }
}
