import freditor.FreditorUI;

import java.io.Writer;

public class FreditorWriter extends Writer {
    private final FreditorUI output;
    public Runnable beforeFirstWrite = null;

    public FreditorWriter(FreditorUI output) {
        super(output); // synchronize on output
        this.output = output;
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        if (beforeFirstWrite != null) {
            beforeFirstWrite.run();
            beforeFirstWrite = null;
        }
        output.append(new ArrayCharSequence(cbuf, off, len));
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
