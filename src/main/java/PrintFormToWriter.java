import java.io.IOException;
import java.io.Writer;

@FunctionalInterface
public interface PrintFormToWriter {
    void print(Object x, Writer w) throws IOException;
}
