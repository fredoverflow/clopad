import clojure.lang.RT;
import freditor.SwingConfig;

import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingConfig.metalWithDefaultFont(SwingConfig.SANS_SERIF_PLAIN_16);
        EventQueue.invokeLater(MainFrame::new);
        RT.init();
    }
}
