import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.function.Consumer;

public class DocumentAdapter implements DocumentListener {
    private final Consumer<DocumentEvent> consumer;

    public DocumentAdapter(Consumer<DocumentEvent> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        consumer.accept(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        consumer.accept(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        consumer.accept(e);
    }
}
