import clojure.lang.RT;
import clojure.lang.Var;
import freditor.FreditorUI;

import javax.swing.*;
import java.io.IOException;
import java.io.StringWriter;

public class Console extends StringWriter {
    private static final int PRINT_LENGTH = 100;

    private final JTabbedPane tabs;
    private final FreditorUI defaultTarget;
    public FreditorUI target;

    public Console(JTabbedPane tabs, FreditorUI defaultTarget) {
        this.tabs = tabs;
        this.defaultTarget = defaultTarget;
    }

    public void run(Runnable body) {
        getBuffer().setLength(0);
        Var.pushThreadBindings(RT.map(RT.OUT, this, Clojure.printLength, PRINT_LENGTH, RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        try {
            target = defaultTarget;
            body.run();
        } catch (Throwable ex) {
            Throwable cause = ex.getCause();
            if (cause == null) {
                cause = ex;
            }
            append(cause.getMessage());
        } finally {
            Var.popThreadBindings();
            target.loadFromString(toString());
            tabs.setSelectedComponent(target);
        }
    }

    public void append(Object obj) {
        if (obj != null) {
            append(obj.toString());
        }
    }

    public void print(Object form) {
        try {
            RT.print(form, this);
        } catch (IOException impossible) {
            impossible.printStackTrace();
        }
    }
}
