import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.Symbol;
import clojure.lang.Var;

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
}
