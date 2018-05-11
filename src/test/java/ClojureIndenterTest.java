import freditor.CharZipper;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class ClojureIndenterTest {
    private static CharZipper text(String s) {
        CharZipper text = new CharZipper(new Flexer());
        text.loadFromString(s);
        return text;
    }

    @Test
    public void square() {
        CharZipper square = text("   ;first line\n       (def square [x]\n (let [y\n" + "(* x x)]\n" + "y))");
        int[] actual = ClojureIndenter.instance.corrections(square);
        int[] expected = {-3, -7, 1, 8, 4};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void tooManyClosingParens() {
        CharZipper closing = text(" )\n  a");
        int[] actual = ClojureIndenter.instance.corrections(closing);
        int[] expected = {-1, -2};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void ignoreQuotedLeadingSpaces() {
        CharZipper quoted = text(" \" \n    quoted leading spaces \"");
        int[] actual = ClojureIndenter.instance.corrections(quoted);
        int[] expected = {-1, 0};
        assertArrayEquals(expected, actual);
    }
}
