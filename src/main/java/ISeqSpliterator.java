import clojure.lang.ISeq;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ISeqSpliterator<T> implements Spliterator<T> {
    private ISeq seq;

    public ISeqSpliterator(ISeq seq) {
        this.seq = seq;
    }

    public static <T> Stream<T> stream(ISeq seq) {
        return StreamSupport.stream(new ISeqSpliterator<>(seq), false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (seq == null) return false;

        action.accept((T) seq.first());
        seq = seq.next();
        return true;
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return IMMUTABLE;
    }
}
