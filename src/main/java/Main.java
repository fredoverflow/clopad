import clojure.lang.RT;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        RT.init();
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
