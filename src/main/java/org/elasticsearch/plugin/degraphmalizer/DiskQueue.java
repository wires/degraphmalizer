/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * User: rico
 * Date: 04/02/2013
 */
public class DiskQueue {
    private static final ESLogger LOG = Loggers.getLogger(DegraphmalizerLifecycleListener.class);

    String name;

    public DiskQueue(String name) {
        this.name = name;
    }

    public synchronized void writeToDisk(BlockingQueue<DelayedImpl<Change>> queue, int limit) throws IOException {
        int count = 0;
        File queueFile = new File(name);
        if (!queueFile.exists() || queueFile.isFile()) {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(queueFile, true)));
            while (queue.size() > 0 && count < limit) {
                try {
                    Change change = queue.take().thing();
                    writer.println(Change.toValue(change));
                    count++;
                } catch (InterruptedException e) {
                    LOG.warn("Take from queue interrupted " + e.getMessage());
                }
            }
            writer.flush();
            writer.close();
        } else {
            LOG.error("Queue file {} is not available or is a directory", name);
        }
    }

    public synchronized void writeToDisk(List<Change> changes) throws IOException {
        File queueFile = new File(name);
        if (!queueFile.exists() || queueFile.isFile()) {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(queueFile, true)));
            for (Change change : changes) {
                writer.println(Change.toValue(change));
            }
            writer.flush();
            writer.close();
        } else {
            LOG.error("Queue file {} is not available or is a directory", name);
        }
    }

    public void writeToDisk(Change change) throws IOException {
        writeToDisk(Arrays.asList(change));
    }

    public synchronized List<Change> readFromDisk() throws IOException {
        List<Change> changes = new ArrayList<Change>();
        File queueFile = new File(name);
        if (queueFile.exists()) {
            if (queueFile.isFile()) {
                BufferedReader reader = new BufferedReader(new FileReader(queueFile));
                String line = reader.readLine();
                do {
                    Change change = Change.fromValue(line);
                    changes.add(change);
                    line = reader.readLine();
                } while (line != null);
                reader.close();
                queueFile.delete();
            } else {
                LOG.error("Queue file {} is not a file", name);
            }
        }
        return changes;
    }

    public synchronized void readFromDiskIntoQueue(BlockingQueue<DelayedImpl<Change>> queue) throws IOException {
        File queueFile = new File(name);
        if (queueFile.exists()) {
            if (queueFile.isFile()) {
                BufferedReader reader = new BufferedReader(new FileReader(queueFile));
                String line = reader.readLine();
                do {
                    Change change = Change.fromValue(line);
                    try {
                        queue.put(DelayedImpl.immediate(change));
                    } catch (InterruptedException e) {
                        LOG.warn("Put into queue was interrupted: {}", e.getMessage());
                    }
                    line = reader.readLine();
                } while (line != null);
                reader.close();
                queueFile.delete();
            } else {
                LOG.error("Queue file {} is not a file", name);
            }
        }
    }

    public String name() {
        return name;
    }
}
