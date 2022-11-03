import clojure.lang.ISeq;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Symbol;
import freditor.Fronts;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.function.BiConsumer;

public class NamespaceExplorer extends JPanel {
    private final JComboBox<Namespace> namespaces;
    private final JList<Symbol> names;
    private final JTextField filter;

    public NamespaceExplorer(BiConsumer<Namespace, Symbol> onSymbolSelected) {
        super(new BorderLayout());

        namespaces = new JComboBox<>();
        namespaces.setFont(Fronts.sansSerif);
        namespaces.addItemListener(event -> filterSymbols());

        names = new JList<>();
        names.setFont(Fronts.sansSerif);
        names.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        names.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) return;

            Object namespace = namespaces.getSelectedItem();
            if (namespace == null) return;

            Symbol symbol = names.getSelectedValue();
            if (symbol == null) return;

            onSymbolSelected.accept((Namespace) namespace, symbol);
        });

        filter = new JTextField();
        filter.setFont(Fronts.sansSerif);
        filter.getDocument().addDocumentListener(new DocumentAdapter(event -> filterSymbols()));

        add(namespaces, BorderLayout.NORTH);
        add(new JScrollPane(names), BorderLayout.CENTER);
        add(filter, BorderLayout.SOUTH);

        updateNamespaces();
    }

    public void updateNamespaces() {
        ISeq allNamespaces = Namespace.all();
        if (RT.count(allNamespaces) != namespaces.getItemCount()) {
            namespaces.removeAllItems();
            ISeqSpliterator.<Namespace>stream(allNamespaces)
                    .sorted(Comparator.comparing(Namespace::toString))
                    .forEach(namespaces::addItem);
        }
    }

    private void filterSymbols() {
        Namespace namespace = (Namespace) namespaces.getSelectedItem();
        if (namespace == null) return;

        String filterText = filter.getText();

        ISeq interns = RT.keys(Clojure.nsInterns.invoke(namespace.name));
        Symbol[] symbols = ISeqSpliterator.<Symbol>stream(interns)
                .filter(symbol -> symbol.getName().contains(filterText))
                .sorted(Comparator.comparing(Symbol::getName))
                .toArray(Symbol[]::new);
        names.setListData(symbols);

        filter.setBackground(filterText.isEmpty() || symbols.length > 0 ? Color.WHITE : Color.PINK);
    }
}
