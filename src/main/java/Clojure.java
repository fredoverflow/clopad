import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class Clojure {
    public static final IFn require;
    public static final IFn symbol;
    public static final IFn sourceFn;
    public static final IFn take;

    static {
        RT.init();

        require = Var.find(Symbol.create("clojure.core", "require"));
        require.invoke(Symbol.create("clojure.repl"));
        require.invoke(Symbol.create("clojure.edn"));

        symbol = Var.find(Symbol.create("clojure.core", "symbol"));
        sourceFn = Var.find(Symbol.create("clojure.repl", "source-fn"));
        take = Var.find(Symbol.create("clojure.core", "take"));
    }
}
