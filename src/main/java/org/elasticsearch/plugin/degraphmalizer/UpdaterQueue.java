package org.elasticsearch.plugin.degraphmalizer;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

public class UpdaterQueue {

    private final ESLogger LOG = Loggers.getLogger(UpdaterQueue.class);

    private final BlockingQueue<DelayedImpl<Change>> inputQueue = new DelayQueue<DelayedImpl<Change>>();
    private final BlockingQueue<DelayedImpl<Change>> outputQueue = new DelayQueue<DelayedImpl<Change>>();
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

        new QueueManager().start();
    }

    public void add(DelayedImpl<Change> delayedChange) {
        inputQueue.add(delayedChange);
    }

    public DelayedImpl<Change> take() throws InterruptedException {
        return outputQueue.take();
    }

    public boolean isEmpty() {
        // TODO: return whether in-memory and disk queues are all empty
    }

    public int size() {
        // TODO: total size of in-memory and disk queues
    }

    public void shutdown() {
        // TODO: flush in-memory queues to disk
    }

    public void clear() {
        // TODO: empty in-memory and disk queues
    }

    class QueueManager extends Thread {

        @Override
        public void run() {
            while (true) {

            }
        }
    }

    class OverflowFileManager {

        private final FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(index + "-overflow\\.log\\.\\d+");
            }
        };

        private final Comparator<File> fileComparator = new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return Long.compare(file1.lastModified(), file2.lastModified());
            }
        };

        /**
         * Number of records in overflow files.
         */
        public int size() {
            return getOverflowFiles().length * limit;
        }

        /**
         * Saves the contents of the input queue to disk.
         */
        public void save() {
            // TODO
        }

        /**
         * Load the contents of the 'first in line' overflow file into the output queue.
         */
        public void load() throws IOException {
            File[] files = getOverflowFiles();

            if (files.length > 0) {
                File file = files[0];
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                do {
                    Change change = Change.fromValue(line);
                    try {
                        outputQueue.put(DelayedImpl.immediate(change));
                    } catch (InterruptedException e) {
                        LOG.warn("Put into queue was interrupted: {}", e.getMessage());
                    }
                    line = reader.readLine();
                } while (line != null);
                reader.close();
                if (!file.delete()) {
                    LOG.error("Can not remove file {}", file.getCanonicalPath());
                }
            }
        }

        /**
         * Array of overflow files sorted by last modified date.
         */
        private File[] getOverflowFiles() {
            final File[] files = new File(logPath).listFiles(filenameFilter);
            Arrays.sort(files, fileComparator);
            return files;
        }
    }
}
