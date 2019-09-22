import freditor.FreditorUI;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Editor extends FreditorUI {
    public static final String directory = System.getProperty("user.home") + File.separator + "clopad" + File.separator;
    public static final String filename = directory + "!clopad.txt";

    public Editor() {
        super(Flexer.instance, ClojureIndenter.instance, 80, 25);
        try {
            loadFromFile(filename);
        } catch (IOException ex) {
            loadFromString(";; F1 (right-click) show source or doc\n"
                    + ";; F5  evaluate whole program\n"
                    + ";; F12 evaluate top-level expression\n"
                    + "(ns user\n  (:require [clojure.string :as string]))\n\n"
                    + "(defn square [x]\n  (* x x))\n\n"
                    + "(->> (range 1 11)\n  (map square )\n  (string/join \", \" ))\n");
        }
    }

    public void tryToSaveCode() {
        createDirectory();
        tryToSaveCodeAs(filename);
        tryToSaveCodeAs(backupFilename());
    }

    private void createDirectory() {
        File dir = new File(directory);
        if (dir.mkdir()) {
            System.out.println("created directory " + dir);
        }
    }

    private void tryToSaveCodeAs(String pathname) {
        try {
            System.out.println("saving code as " + pathname);
            saveToFile(pathname);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String backupFilename() {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA");
            byte[] text = getText().getBytes(StandardCharsets.ISO_8859_1);
            byte[] hash = sha1.digest(text);
            StringBuilder builder = new StringBuilder(directory);
            for (byte x : hash) {
                builder.append("0123456789abcdef".charAt((x >>> 4) & 15));
                builder.append("0123456789abcdef".charAt(x & 15));
            }
            return builder.append(".txt").toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new RuntimeException(impossible);
        }
    }
}
