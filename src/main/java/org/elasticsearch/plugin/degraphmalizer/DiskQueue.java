/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
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

    public void writeToDisk(BlockingQueue<DelayedImpl<Change>> queue, int limit) throws IOException, InterruptedException {
        int count=0;
        Path queueFile = FileSystems.getDefault().getPath(name);
        if (!Files.exists(queueFile) || Files.isRegularFile(queueFile)) {
            BufferedWriter writer = Files.newBufferedWriter(queueFile, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
            while (queue.size()>0 && count < limit) {
                Change change = queue.take().thing();
                writer.write(Change.toValue(change));
                writer.newLine();
                count++;
            }
            writer.flush();
            writer.close();
        } else {
            LOG.error("Queue file {} is not available or is a directory",name);
        }
    }

    public List<Change> readFromDisk() throws IOException {
        List<Change> changes = new ArrayList<Change>();
        Path queueFile = FileSystems.getDefault().getPath(name);
        if (Files.exists(queueFile)) {
            if (Files.isRegularFile(queueFile)) {
                BufferedReader reader = Files.newBufferedReader(queueFile,Charset.forName("UTF-8"));
                String line = reader.readLine();
                do {
                   Change change = Change.fromValue(line);
                   changes.add(change);
                   line = reader.readLine();
                } while (line!=null);
                reader.close();
                Files.delete(queueFile);
            } else {
                LOG.error("Queue file {} is not a file",name);
            }
        }
        return changes;
    }
}
