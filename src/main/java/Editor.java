import freditor.FreditorUI;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Editor extends FreditorUI {
    public Editor() {
        super(Flexer.instance, ClojureIndenter.instance, 80, 25);
        try {
            loadFromFile(filename);
        } catch (IOException ex) {
            loadFromString(";; F1 (right-click) show source or doc\n"
                    + ";; F5  evaluate whole program\n"
                    + ";; F12 evaluate form at cursor\n"
                    + "(ns user (:require [clojure.string :as string]))\n\n"
                    + "(defn square [x] (* x x))\n\n(->>\n  (range 1 11)\n  (map square )\n  (string/join \", \" ))");
        }
    }

    public static final String directory = System.getProperty("user.home") + "/clopad";
    public static final String filenamePrefix = directory + "/clopad";
    public static final String filenameSuffix = ".txt";
    public static final String filename = filenamePrefix + filenameSuffix;

    public void tryToSaveCode() {
        createDirectory();
        tryToSaveCodeAs(filename);
        tryToSaveCodeAs(backupFilename());
    }

    private void createDirectory() {
        if (new File(directory).mkdir()) {
            System.out.println("created directory " + directory);
        }
    }

    private void tryToSaveCodeAs(String pathname) {
        try {
            System.out.println("saving code as " + pathname);
            saveToFile(pathname);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private String backupFilename() {
        try {
            byte[] hash = MessageDigest.getInstance("SHA").digest(getText().getBytes());
            StringBuilder sb = new StringBuilder(filenamePrefix);
            sb.append('_');
            for (byte b : hash) {
                sb.append("0123456789abcdef".charAt((b >>> 4) & 15));
                sb.append("0123456789abcdef".charAt(b & 15));
            }
            return sb.append(filenameSuffix).toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new RuntimeException(impossible);
        }
    }
}
