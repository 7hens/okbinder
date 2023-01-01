package cn.thens.okbinder2;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class IndexedData<T> {
    public final T data;
    public final int index;

    IndexedData(T data, int index) {
        this.data = data;
        this.index = index;
    }

    public boolean isFirst() {
        return index == 0;
    }

    public <R> IndexedData<R> to(R value) {
        return new IndexedData<>(value, index);
    }

    public static <T> Stream<IndexedData<T>> stream(Stream<T> stream) {
        AtomicInteger index = new AtomicInteger();
        return stream.map(m -> new IndexedData<>(m, index.getAndIncrement()));
    }

    public static <T> Stream<IndexedData<T>> stream(Collection<T> collection) {
        return stream(collection.stream());
    }
}
