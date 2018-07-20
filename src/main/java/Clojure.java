import clojure.lang.IFn;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class Clojure {
    public static final Symbol ns;
    public static final Var printLength;

    public static final IFn nsInterns;
    public static final IFn sourceFn;
    public static final IFn symbol;

    static {
        IFn require = Var.find(Symbol.create("clojure.core", "require"));
        require.invoke(Symbol.create("clojure.repl"));

        ns = Symbol.create(null, "ns");
        printLength = Var.find(Symbol.create("clojure.core", "*print-length*"));

        nsInterns = Var.find(Symbol.create("clojure.core", "ns-interns"));
        sourceFn = Var.find(Symbol.create("clojure.repl", "source-fn"));
        symbol = Var.find(Symbol.create("clojure.core", "symbol"));
    }
}
