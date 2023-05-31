import clojure.lang.Compiler;
import clojure.lang.*;
import freditor.FreditorUI;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.function.Supplier;

public class Console {
    private static final int PRINT_LENGTH = 100;

    private final JTabbedPane tabs;
    private final FreditorUI output;
    private final Supplier<FreditorUI> input;

    private final FreditorWriter freditorWriter;
    public final PrintWriter printWriter;

    public Console(JTabbedPane tabs, FreditorUI output, Supplier<FreditorUI> input) {
        this.tabs = tabs;
        this.output = output;
        this.input = input;

        freditorWriter = new FreditorWriter(output);
        printWriter = new PrintWriter(freditorWriter);

        Compiler.SOURCE_PATH.bindRoot(input.get().getFile().toString());
        Compiler.SOURCE.bindRoot(input.get().getFile().getFileName().toString());
        RT.OUT.bindRoot(printWriter);
        RT.ERR.bindRoot(printWriter);
        Clojure.printLength.bindRoot(PRINT_LENGTH);
    }

    public void run(boolean setCursor, Runnable body) {
        freditorWriter.beforeFirstWrite = () -> {
            output.load("");
            tabs.setSelectedComponent(output);
        };
        Var.pushThreadBindings(RT.map(RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        try {
            input.get().saveWithBackup();
            body.run();
        } catch (Compiler.CompilerException ex) {
            Throwable cause = ex.getCause();
            StackTraceElement[] fullStackTrace = cause.getStackTrace();
            StackTraceElement[] userStackTrace = Arrays.stream(fullStackTrace)
                    .filter(element -> input.get().getFile().getFileName().toString().equals(element.getFileName()))
                    .toArray(StackTraceElement[]::new);
            if (userStackTrace.length > 0) {
                printStackTrace(cause, userStackTrace);
                if (setCursor) {
                    int line = userStackTrace[0].getLineNumber();
                    input.get().setCursorTo(line - 1, 0);
                }
                printStackTrace(cause, fullStackTrace);
            } else {
                printStackTrace(cause, fullStackTrace);
                if (setCursor && ex.line > 0) {
                    IPersistentMap data = ex.getData();
                    Integer column = (Integer) data.valAt(Compiler.CompilerException.ERR_COLUMN);
                    input.get().setCursorTo(ex.line - 1, column - 1);
                }
            }
        } finally {
            Var.popThreadBindings();
        }
    }

    private void printStackTrace(Throwable cause, StackTraceElement[] stackTrace) {
        printWriter.println(cause.getClass().getName());
        printWriter.println(cause.getLocalizedMessage());
        for (StackTraceElement element : stackTrace) {
            // trim stack trace at first Clopad type (default package; unqualified class name)
            if (Character.isUpperCase(element.getClassName().charAt(0))) break;
            printWriter.println("\tat " + element);
        }
        printWriter.println();
    }

    public void print(PrintFormToWriter printFormToWriter, String prefix, Object form, String suffix) {
        StringWriter stringWriter = new StringWriter();
        try {
            stringWriter.append(prefix);
            printFormToWriter.print(form, stringWriter);
            ensureEmptyLine(stringWriter);
            stringWriter.append(suffix);
        } catch (IOException ex) {
            throw Util.sneakyThrow(ex);
        } finally {
            printWriter.append(stringWriter.toString());
        }
    }

    private void ensureEmptyLine(StringWriter stringWriter) {
        StringBuffer buffer = stringWriter.getBuffer();
        int length = buffer.length();
        if (length > 0 && buffer.charAt(length - 1) != '\n') {
            stringWriter.append('\n');
        }
    }
}
