package org.elasticsearch.plugin.degraphmalizer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

public class UpdaterQueue {

    private final BlockingQueue<DelayedImpl<Change>> queue = new DelayQueue<DelayedImpl<Change>>();
    private DiskQueue overflowFile; // TODO: multiple

    private final String index;
    private final int limit;
    private final String logPath;

    // Signals if the memory queue is not available and that changes are written to disk.
    private volatile boolean overflowActive = false;

    public UpdaterQueue(final String index, final int limit, final String logPath) {
        this.index = index;
        this.limit = limit;
        this.logPath = logPath;
    }

    public void add(DelayedImpl<Change> delayedChange) {
        // TODO
    }

    public DelayedImpl<Change> take()
    {
        // TODO
    }

    public boolean isEmpty() {
        // TODO
    }

    public int size() {
        // TODO
    }

    public void shutdown() {
        // TODO
    }

    public void clear() {
        // TODO
    }
}
