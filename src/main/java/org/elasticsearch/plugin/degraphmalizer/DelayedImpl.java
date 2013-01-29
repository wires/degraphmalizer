package org.elasticsearch.plugin.degraphmalizer;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A delayed generic thing.
 *
 * @param <T> the type of the thing.
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class DelayedImpl<T> implements Delayed {
    private final T thing;
    private final long delayInMillis;
    private final long baseMillis;

    public DelayedImpl(T thing, long delayInMillis) {
        this.thing = thing;
        this.delayInMillis = delayInMillis;
        this.baseMillis = System.currentTimeMillis();
    }

    public T thing() {
        return thing;
    }

    @Override
    public long getDelay(TimeUnit timeUnit) {
        return timeUnit.convert(delayInMillis - (System.currentTimeMillis() - baseMillis), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }

    public static <T> DelayedImpl<T> immediate(T thing) {
        return new DelayedImpl<T>(thing, 0);
    }
}
