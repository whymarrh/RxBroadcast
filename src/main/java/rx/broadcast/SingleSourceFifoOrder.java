package rx.broadcast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

public final class SingleSourceFifoOrder<T> implements BroadcastOrder<T> {
    public static final boolean DROP_LATE = true;

    private final Map<Integer, SortedSet<Timestamped<T>>> pendingQueues;

    private long expectedTimestamp;

    private final boolean dropLateMessages;

    public SingleSourceFifoOrder() {
        this(false);
    }

    public SingleSourceFifoOrder(final boolean dropLateMessages) {
        this.pendingQueues = new HashMap<>();
        this.dropLateMessages = dropLateMessages;
    }

    @Override
    public void receive(final int sender, final Consumer<T> consumer, final Timestamped<T> value) {
        if (dropLateMessages) {
            if (Long.compareUnsigned(value.timestamp, expectedTimestamp) >= 0) {
                consumer.accept(value.value);
                expectedTimestamp = value.timestamp + 1;
            }

            return;
        }

        final SortedSet<Timestamped<T>> queue = pendingQueues.computeIfAbsent(sender, k -> new TreeSet<>());

        if (value.timestamp == expectedTimestamp) {
            consumer.accept(value.value);
            expectedTimestamp = value.timestamp + 1;
        } else {
            queue.add(value);
        }

        final Iterator<Timestamped<T>> iterator = queue.iterator();
        while (iterator.hasNext()) {
            final Timestamped<T> tv = iterator.next();
            if (tv.timestamp > expectedTimestamp) {
                break;
            }

            consumer.accept(tv.value);
            expectedTimestamp = tv.timestamp + 1;
            iterator.remove();
        }
    }
}
