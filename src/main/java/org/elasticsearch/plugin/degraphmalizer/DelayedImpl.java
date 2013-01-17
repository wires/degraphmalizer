package org.elasticsearch.plugin.degraphmalizer;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A delayed generic thing.
 *
 * @param <T> the type of the thing.
 */
public class DelayedImpl<T> implements Delayed
{
    private final T thing;
    private final long delayInMillis;

    public DelayedImpl(T thing, long delayInMillis)
    {
        this.thing = thing;
        this.delayInMillis = delayInMillis;
    }

    public T thing()
    {
        return thing;
    }

    @Override
    public long getDelay(TimeUnit timeUnit)
    {
        return timeUnit.convert(delayInMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other)
    {
        return Long.compare(delayInMillis, other.getDelay(TimeUnit.MILLISECONDS));
    }

    public static <T> DelayedImpl<T> immediate(T thing)
    {
        return new DelayedImpl<T>(thing, 0);
    }
}
