import freditor.Autosaver;
import freditor.FreditorUI;

public class Editor extends FreditorUI {
    public final Autosaver autosaver;

    public Editor() {
        super(Flexer.instance, ClojureIndenter.instance, 80, 25);
        autosaver = newAutosaver("clopad");
        autosaver.loadOrDefault(""
                + ";; F1  show source or doc-string (same as right-click)\n"
                + ";; F5  evaluate    whole program\n"
                + ";; F11 macroexpand top-level form (CTRL also expands nested macros)\n"
                + ";; F12 evaluate    top-level form\n"
                + "(ns user\n  (:require [clojure.string :as string]))\n\n"
                + "(defn square [x]\n  (* x x))\n\n"
                + "(->> (range 1 11)\n  (map square )\n  (string/join \", \" ))\n");
    }
}
