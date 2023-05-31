import clojure.lang.Compiler;
import clojure.lang.*;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.function.Consumer;

import static clojure.lang.Compiler.*;

public class Clojure {
    public static final Keyword doc;
    public static final Symbol null_ns;
    public static final Symbol clojure_core_ns;
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
        null_ns = Symbol.create(null, "ns");
        clojure_core_ns = Symbol.create("clojure.core", "ns");
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

    public static void loadFromScratch(String text, Path file,
                                       Consumer<Object> resultContinuation) {
        Object[] result = new Object[1];
        feedFormsBefore(text, file, Integer.MAX_VALUE, Integer.MAX_VALUE, form -> {
            if (isNamespaceForm(form)) {
                Namespace.remove((Symbol) ((PersistentList) form).next().first());
            }
            result[0] = Compiler.eval(form, false);
        }, formAtCursor -> {
            resultContinuation.accept(result[0]);
        });
    }

    public static boolean isNamespaceForm(Object form) {
        if (!(form instanceof PersistentList)) return false;
        Object first = ((PersistentList) form).first();
        return null_ns.equals(first) || clojure_core_ns.equals(first);
    }

    public static void evaluateNamespaceFormsBefore(String text, Path file,
                                                    int row, int column,
                                                    Runnable updateNamespaces,
                                                    Consumer<Object> formContinuation) {
        feedFormsBefore(text, file, row, column, form -> {
            if (isNamespaceForm(form)) {
                Compiler.eval(form, false);
                updateNamespaces.run();
            }
        }, formContinuation);
    }

    private static void feedFormsBefore(String text, Path file,
                                        int row, int column,
                                        Consumer<Object> formConsumer,
                                        Consumer<Object> formContinuation) {
        LineNumberingPushbackReader reader = new LineNumberingPushbackReader(new StringReader(text));

        Var.pushThreadBindings(RT.mapUniqueKeys(LOADER, RT.makeClassLoader(),
                SOURCE_PATH, file.toString(),
                SOURCE, file.getFileName().toString(),
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
            throw new CompilerException(file.toString(), ex.line, ex.column, null, CompilerException.PHASE_READ, ex.getCause());
        } catch (CompilerException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new CompilerException(file.toString(), (Integer) LINE.deref(), (Integer) COLUMN.deref(), ex);
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
