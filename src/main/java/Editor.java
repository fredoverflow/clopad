import freditor.Autosaver;
import freditor.FreditorUI;

public class Editor extends FreditorUI {
    public final Autosaver autosaver;

    public Editor() {
        super(Flexer.instance, ClojureIndenter.instance, 80, 25);
        autosaver = newAutosaver("clopad");
        autosaver.loadOrDefault(";; F1 (right-click) show source or doc\n"
                + ";; F5  evaluate whole program\n"
                + ";; F12 evaluate top-level expression\n"
                + "(ns user\n  (:require [clojure.string :as string]))\n\n"
                + "(defn square [x]\n  (* x x))\n\n"
                + "(->> (range 1 11)\n  (map square )\n  (string/join \", \" ))\n");
    }
}
