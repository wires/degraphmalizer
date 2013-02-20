package org.elasticsearch.plugin.degraphmalizer.updater;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;

public class UpdaterOverflowFileManager {

    private static final ESLogger LOG = Loggers.getLogger(UpdaterOverflowFileManager.class);

    private static final File[] NO_FILES = {};

    private final String logPath;
    private final String filenamePrefix;
    private final int limit;

    public UpdaterOverflowFileManager(final String logPath, final String index, final int limit) {
        this.logPath = logPath;
        this.filenamePrefix = index + "-overflow-";
        this.limit = limit;
    }

    private final FilenameFilter filenameFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.matches(filenamePrefix + "\\d+");
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
        int count = 0;
        for (File file : getOverflowFiles()) {
            count += countLines(file);
        }
        return count;
    }

    public int countLines(File file) {
        try {
            LineNumberReader lnr = new LineNumberReader(new FileReader(file));
            lnr.skip(Long.MAX_VALUE);
            lnr.getLineNumber();
        } catch (IOException e) {
            LOG.error("Can't read from file {} " + file.getPath());
        }
        return 0;
    }

    public boolean isEmpty() {
        return getOverflowFiles().length == 0;
    }

    public void clear() {
        for (File file : getOverflowFiles()) {
            if (!file.delete()) {
                try {
                    LOG.error("Error deleting file {}", file.getCanonicalPath());
                } catch (IOException e) {
                    LOG.error("Error deleting file {}", file.getName());
                }
            }
        }
    }

    /**
     * Saves the contents of the input queue to disk.
     */
    public void save(final BlockingQueue<DelayedImpl<Change>> queue) {
        File file;
        do {
            file = new File(logPath, filenamePrefix + System.currentTimeMillis());
        } while (file.exists());

        try {
            final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
            int count = 0;
            while (!queue.isEmpty() && count < limit) {
                try {
                    final DelayedImpl<Change> delayed = queue.take();
                    writer.println(delayed.toValue());
                    count++;
                } catch (InterruptedException e) {
                    LOG.warn("Take from input queue interrupted: " + e.getMessage());
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            LOG.error("Error saving overflow file {}: {}", file, e.getMessage());
        }
    }

    /**
     * Load the contents of the 'first in line' overflow file into the output queue.
     */
    public void load(final BlockingQueue<DelayedImpl<Change>> queue) {
        final File[] files = getOverflowFiles();
        final DelayedImpl<Change> delayedFactory = new DelayedImpl<Change>(new Change(), 0);

        if (files.length > 0) {
            final File file = files[0];
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                String line = reader.readLine();
                do {
                    try {
                        final DelayedImpl<Change> delayed = delayedFactory.fromValue(line);
                        queue.put(delayed);
                    } catch (InterruptedException e) {
                        LOG.warn("Put into queue was interrupted: {}", e.getMessage());
                    } catch (Exception e) {
                        LOG.error("Unparsable overflow line " + line);
                    }
                    line = reader.readLine();
                } while (line != null);
                reader.close();
                if (!file.delete()) {
                    LOG.error("Can not remove file {}", file.getCanonicalPath());
                }
            } catch (IOException e) {
                LOG.error("Error loading overflow file {}: {}", file, e.getMessage());
            }
        }
    }

    /**
     * Array of overflow files sorted by last modified date.
     */
    private File[] getOverflowFiles() {
        final File[] files = new File(logPath).listFiles(filenameFilter);
        if (files != null) {
            Arrays.sort(files, fileComparator);
        } else {
            return NO_FILES;
        }
        return files;
    }
}