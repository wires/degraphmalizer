package org.elasticsearch.plugin.degraphmalizer.updater;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A delayed generic thing.
 *
 * @param <T> the type of the thing.
 *            <p/>
 *            Note: this class has a natural ordering that is inconsistent with equals.
 */
public class DelayedImpl<T extends StringSerialization<T>> implements Delayed, StringSerialization<DelayedImpl<T>> {
    private final T thing;
    private final long delayInMillis;
    private final long baseMillis;

    public DelayedImpl(final T thing, final long delayInMillis) {
        this(thing, delayInMillis, System.currentTimeMillis());
    }

    public DelayedImpl(final T thing, final long delayInMillis, final long baseMillis) {
        this.thing = thing;
        this.delayInMillis = delayInMillis;
        this.baseMillis = baseMillis;
    }

    public T thing() {
        return thing;
    }

    @Override
    public long getDelay(final TimeUnit timeUnit) {
        return timeUnit.convert(delayInMillis - (System.currentTimeMillis() - baseMillis), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(final Delayed other) {
        return Long.valueOf(getDelay(TimeUnit.MILLISECONDS)).compareTo(other.getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DelayedImpl delayed = (DelayedImpl) o;

        if (baseMillis != delayed.baseMillis) return false;
        if (delayInMillis != delayed.delayInMillis) return false;
        if (!thing.equals(delayed.thing)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = thing.hashCode();
        result = 31 * result + (int) (delayInMillis ^ (delayInMillis >>> 32));
        result = 31 * result + (int) (baseMillis ^ (baseMillis >>> 32));
        return result;
    }

    public static <T extends StringSerialization<T>> DelayedImpl<T> immediate(final T thing) {
        return new DelayedImpl<T>(thing, 0);
    }

    @Override
    public String toValue() {
        return delayInMillis + "," + baseMillis + "," + thing().toValue();
    }

    @Override
    public DelayedImpl<T> fromValue(String value) {
        String[] values = value.split(",", 3);
        Long delay = Long.valueOf(values[0]);
        Long base = Long.valueOf(values[1]);
        T thing = thing().fromValue(values[2]);
        return new DelayedImpl<T>(thing, delay, base);
    }
}
