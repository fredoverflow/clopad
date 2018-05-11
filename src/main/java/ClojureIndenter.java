import freditor.CharZipper;
import freditor.Indenter;

import java.util.ArrayDeque;

public class ClojureIndenter extends Indenter {
    public static final ClojureIndenter instance = new ClojureIndenter();

    @Override
    public int[] corrections(CharZipper text) {
        final int rows = text.rows();
        int[] corrections = new int[rows];
        ArrayDeque<Integer> indentations = new ArrayDeque<>();
        int indentation = 0;
        int i = 0;
        for (int row = 0; row < rows; ++row) {
            int column = text.leadingSpaces(i);
            i += column;
            int correction = indentation - column;
            corrections[row] = correction;
            final int columns = text.lengthOfRow(row);
            for (; column < columns; ++column) {
                int state = text.intAt(i++) >> 16;
                switch (state) {
                    case Flexer.OPENING_BRACE:
                    case Flexer.OPENING_BRACKET:
                        indentations.push(indentation);
                        indentation = column + correction + 1;
                        break;

                    case Flexer.OPENING_PAREN:
                        indentations.push(indentation);
                        indentation = column + correction + 2;
                        break;

                    case Flexer.CLOSING_BRACE:
                    case Flexer.CLOSING_BRACKET:
                    case Flexer.CLOSING_PAREN:
                        Integer top = indentations.poll();
                        indentation = (top != null) ? top : 0;
                        break;
                }
            }
            ++i; // new line
        }
        return corrections;
    }
}
