import freditor.FreditorUI;

import java.io.Writer;

public class FreditorWriter extends Writer {
    private final FreditorUI output;

    public FreditorWriter(FreditorUI output) {
        super(output); // synchronize on output
        this.output = output;
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        output.append(new ArrayCharSequence(cbuf, off, len));
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
