import freditor.Freditor;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class ClojureIndenterTest {
    private static Freditor text(String s) {
        Freditor text = new Freditor(Flexer.instance, ClojureIndenter.instance, null);
        text.load(s);
        return text;
    }

    @Test
    public void square() {
        Freditor square = text("   ;first line\n       (def square [x]\n (let [y\n" + "(* x x)]\n" + "y))");
        int[] actual = ClojureIndenter.instance.corrections(square);
        int[] expected = {-3, -7, 1, 8, 4};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void tooManyClosingParens() {
        Freditor closing = text(" )\n  a");
        int[] actual = ClojureIndenter.instance.corrections(closing);
        int[] expected = {-1, -2};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void namespace() {
        Freditor text = text("(ns user\n(:require [clojure.string :as string]\n[clojure.test :refer [deftest is are run-tests]]))");
        int[] actual = ClojureIndenter.instance.corrections(text);
        int[] expected = {0, 2, 12};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void newLineAfterKeyword() {
        Freditor text = text("(:count\nsession\n0)");
        int[] actual = ClojureIndenter.instance.corrections(text);
        int[] expected = {0, 2, 2};
        assertArrayEquals(expected, actual);
    }
}
