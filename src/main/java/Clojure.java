import clojure.lang.Compiler;
import clojure.lang.*;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.function.Consumer;

import static clojure.lang.Compiler.*;

public class Clojure {
    public static final Keyword doc;
    public static final Symbol ns;
    public static final Var printLength;
    public static final Var warnOnReflection;

    public static final IFn macroexpand;
    public static final IFn macroexpandAll;
    public static final IFn nsInterns;
    public static final IFn pprint;
    public static final IFn sourceFn;

    static {
        IFn require = Var.find(Symbol.create("clojure.core", "require"));
        require.invoke(Symbol.create("clojure.pprint"));
        require.invoke(Symbol.create("clojure.repl"));
        require.invoke(Symbol.create("clojure.walk"));

        doc = Keyword.intern("doc");
        ns = Symbol.create(null, "ns");
        printLength = Var.find(Symbol.create("clojure.core", "*print-length*"));
        warnOnReflection = Var.find(Symbol.create("clojure.core", "*warn-on-reflection*"));

        macroexpand = Var.find(Symbol.create("clojure.core", "macroexpand"));
        macroexpandAll = Var.find(Symbol.create("clojure.walk", "macroexpand-all"));
        nsInterns = Var.find(Symbol.create("clojure.core", "ns-interns"));
        pprint = Var.find(Symbol.create("clojure.pprint", "pprint"));
        sourceFn = Var.find(Symbol.create("clojure.repl", "source-fn"));
    }

    public static String firstForm(String text) {
        LineNumberingPushbackReader reader = new LineNumberingPushbackReader(new StringReader(text));
        reader.captureString();
        LispReader.read(reader, false, null, false, null);
        return reader.getString();
    }

    public static void loadFromScratch(String text, String pathname, String filename,
                                       Consumer<Object> resultContinuation) {
        Object[] result = new Object[1];
        feedFormsBefore(text, pathname, filename, Integer.MAX_VALUE, Integer.MAX_VALUE, form -> {
            if (isNamespaceForm(form)) {
                Namespace.remove((Symbol) ((PersistentList) form).next().first());
            }
            result[0] = Compiler.eval(form, false);
        }, formAtCursor -> {
            resultContinuation.accept(result[0]);
        });
    }

    public static boolean isNamespaceForm(Object form) {
        return form instanceof PersistentList && ((PersistentList) form).first().equals(ns);
    }

    public static void evaluateNamespaceFormsBefore(String text, String pathname, String filename,
                                                    int row, int column,
                                                    Runnable updateNamespaces,
                                                    Consumer<Object> formContinuation) {
        feedFormsBefore(text, pathname, filename, row, column, form -> {
            if (isNamespaceForm(form)) {
                Compiler.eval(form, false);
                updateNamespaces.run();
            }
        }, formContinuation);
    }

    private static void feedFormsBefore(String text, String pathname, String filename,
                                        int row, int column,
                                        Consumer<Object> formConsumer,
                                        Consumer<Object> formContinuation) {
        LineNumberingPushbackReader reader = new LineNumberingPushbackReader(new StringReader(text));

        Var.pushThreadBindings(RT.mapUniqueKeys(LOADER, RT.makeClassLoader(),
                SOURCE_PATH, pathname,
                SOURCE, filename,
                METHOD, null,
                LOCAL_ENV, null,
                LOOP_LOCALS, null,
                NEXT_LOCAL_NUM, 0,
                RT.READEVAL, RT.T,
                RT.CURRENT_NS, RT.CURRENT_NS.deref(),
                LINE, -1,
                COLUMN, -1,
                RT.UNCHECKED_MATH, RT.UNCHECKED_MATH.deref(),
                warnOnReflection, warnOnReflection.deref(),
                RT.DATA_READERS, RT.DATA_READERS.deref()));
        try {
            Object form = null;
            long rowColumn = combine(row, column);
            while (skipWhitespace(reader) != -1 && combine(reader.getLineNumber(), reader.getColumnNumber()) <= rowColumn) {
                LINE.set(reader.getLineNumber());
                COLUMN.set(reader.getColumnNumber());
                form = LispReader.read(reader, false, null, false, null);
                formConsumer.accept(form);
            }
            formContinuation.accept(form);
        } catch (LispReader.ReaderException ex) {
            throw new CompilerException(pathname, ex.line, ex.column, null, CompilerException.PHASE_READ, ex.getCause());
        } catch (CompilerException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new CompilerException(pathname, (Integer) LINE.deref(), (Integer) COLUMN.deref(), ex);
        } finally {
            Var.popThreadBindings();
        }
    }

    private static long combine(int hi, int lo) {
        return (long) hi << 32 | lo;
    }

    private static int skipWhitespace(PushbackReader reader) {
        try {
            int ch;
            do {
                ch = reader.read();
            } while (Character.isWhitespace(ch) || ch == ',');
            if (ch != -1) {
                reader.unread(ch);
            }
            return ch;
        } catch (IOException ex) {
            throw Util.sneakyThrow(ex);
        }
    }
}
