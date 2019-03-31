import freditor.FlexerState;
import freditor.Freditor;
import freditor.Indenter;
import freditor.ephemeral.IntStack;

public class ClojureIndenter extends Indenter {
    public static final ClojureIndenter instance = new ClojureIndenter();

    @Override
    public int[] corrections(Freditor text) {
        final int len = text.length();
        final int rows = text.rows();
        int[] corrections = new int[rows];
        IntStack indentations = new IntStack();
        int indentation = 0;
        int i = 0;
        for (int row = 0; row < rows; ++row) {
            int column = text.leadingSpaces(i);
            i += column;
            int correction = indentation - column;
            corrections[row] = correction;
            final int columns = text.lengthOfRow(row);
            for (; column < columns; ++column) {
                FlexerState state = text.stateAt(i++);
                if (state == Flexer.OPENING_BRACE || state == Flexer.OPENING_BRACKET) {
                    indentations.push(indentation);
                    indentation = column + correction + 1;
                } else if (state == Flexer.OPENING_PAREN) {
                    indentations.push(indentation);
                    indentation = column + correction + 2;

                    if (i < len && text.stateAt(i) == Flexer.KEYWORD_HEAD) {
                        final int start = i;
                        i = skipKeywordAndSpaces(text, i);
                        column += i - start;

                        if (text.stateAt(i) != Flexer.NEWLINE) {
                            indentation = column + 1 + correction;
                        }
                    }
                } else if (state == Flexer.CLOSING_BRACE || state == Flexer.CLOSING_BRACKET || state == Flexer.CLOSING_PAREN) {
                    indentation = indentations.isEmpty() ? 0 : indentations.pop();
                }
            }
            ++i; // new line
        }
        return corrections;
    }

    private int skipKeywordAndSpaces(Freditor text, int i) {
        while (text.stateAt(++i) == Flexer.KEYWORD_TAIL) {
        }
        if (text.stateAt(i) == Flexer.SPACE_HEAD) {
            while (text.stateAt(++i) == Flexer.SPACE_TAIL) {
            }
        }
        return i;
    }
}
