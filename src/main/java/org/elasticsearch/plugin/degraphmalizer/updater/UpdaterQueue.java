package org.elasticsearch.plugin.degraphmalizer.updater;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class UpdaterQueue implements Runnable,UpdaterQueueMBean {

    private static final ESLogger LOG = Loggers.getLogger(UpdaterQueue.class);

    private final BlockingQueue<DelayedImpl<Change>> inputQueue = new LinkedBlockingQueue<DelayedImpl<Change>>();
    private final BlockingQueue<DelayedImpl<Change>> outputQueue = new DelayQueue<DelayedImpl<Change>>();

    private final UpdaterOverflowFileManager overflowFileManager;

    private int limit;

    private String index;

    private boolean shuttingDown = false;

    public UpdaterQueue(final String logPath, final String index, final int limit) {
        this.limit = limit/2;
        this.index = index;
        this.overflowFileManager = new UpdaterOverflowFileManager(logPath, index, limit);
        registerMBean();
    }

    @Override
    public void run() {
        while (!shuttingDown) {
            if (outputQueue.size() < limit) {
                if (outputQueue.isEmpty() && !overflowFileManager.isEmpty()) {
                    overflowFileManager.load(outputQueue);
                } else {
                    try {
                        final DelayedImpl<Change> delayedChange = inputQueue.poll(1, TimeUnit.SECONDS); // Use poll() instead of take() to not miss the shutdown flag getting flipped to true.
                        if (delayedChange != null) {
                            outputQueue.add(delayedChange);
                        }
                    } catch (InterruptedException e) {
                        LOG.warn("Getting change from input queue interrupted: " + e.getMessage());
                    }
                }
            } else {
                if (inputQueue.size() >= limit) {
                    overflowFileManager.save(inputQueue);
                }
            }
        }
        flushInMemoryQueuesToDisk();
    }

    public void add(final DelayedImpl<Change> change) {
        inputQueue.add(change);
    }

    public DelayedImpl<Change> take() throws InterruptedException {
        return outputQueue.take();
    }

    public boolean isEmpty() {
        return inputQueue.isEmpty() && outputQueue.isEmpty() && overflowFileManager.isEmpty();
    }

    @Override
    public int size() {
        return inputQueue.size() + outputQueue.size() + overflowFileManager.size();
    }

    @Override
    public int getInputQueueSize() {
        return inputQueue.size();
    }

    @Override
    public int getOutputQueueSize() {
        return outputQueue.size();
    }

    @Override
    public int getOverflowSize() {
        return overflowFileManager.size();
    }

    @Override
    public String getIndex() {
        return index;
    }

    public void shutdown() {
        shuttingDown = true; // Flag for thread to shut down
    }

    @Override
    public void clear() {
        inputQueue.clear();
        outputQueue.clear();
        overflowFileManager.clear();
    }

    private void flushInMemoryQueuesToDisk() {
        while (!outputQueue.isEmpty()) {
            overflowFileManager.save(outputQueue);
        }

        while (!inputQueue.isEmpty()) {
            overflowFileManager.save(inputQueue);
        }
    }


    private void registerMBean() {
        try {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName("org.elasticsearch.plugin.degraphmalizer.updater.UpdaterQueue:type=UpdaterQueue,name=UpdaterQueue-"+index);
            mbs.registerMBean(this, name);
            LOG.info("Registered MBean");
        } catch (Exception e) {
            LOG.error("Failed to register MBean", e);
        }
    }
}
