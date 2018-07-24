import clojure.lang.Symbol;
import freditor.Flexer;
import freditor.FreditorUI;
import freditor.Indenter;

public class FreditorUI_symbol extends FreditorUI {
    public final Symbol symbol;

    public FreditorUI_symbol(Flexer flexer, Indenter indenter, int columns, int rows, Symbol symbol) {
        super(flexer, indenter, columns, rows);
        this.symbol = symbol;
    }
}
