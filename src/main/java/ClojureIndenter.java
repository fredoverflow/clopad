import freditor.CharZipper;
import freditor.Indenter;

import java.util.ArrayDeque;

public class ClojureIndenter implements Indenter {
    public static final ClojureIndenter instance = new ClojureIndenter();

    @Override
    public int[] corrections(CharZipper text) {
        final int rows = text.rows();
        int[] corrections = new int[rows];
        ArrayDeque<Integer> indentations = new ArrayDeque<>();
        int indentation = 0;
        int i = 0;
        for (int row = 0; row < rows; ++row) {
            int correction = indentation - text.leadingSpaces(i);
            corrections[row] = correction; // TODO leading closers?
            final int columns = text.lengthOfRow(row);
            for (int column = 0; column < columns; ++column) {
                int state = text.intAt(i++) >> 16;
                switch (state) {
                    case Flexer.OPENING_BRACE:
                    case Flexer.OPENING_BRACKET:
                        indentation = column + correction + 1;
                        indentations.push(indentation);
                        break;

                    case Flexer.OPENING_PAREN:
                        indentation = column + correction + 2;
                        indentations.push(indentation);
                        break;

                    case Flexer.CLOSING_BRACE:
                    case Flexer.CLOSING_BRACKET:
                    case Flexer.CLOSING_PAREN:
                        indentation = 0;
                        if (!indentations.isEmpty()) {
                            indentations.pop();
                            Integer top = indentations.peek();
                            if (top != null) {
                                indentation = top;
                            }
                        }
                        break;
                }
            }
            ++i; // new line
        }
        return corrections;
    }
}
