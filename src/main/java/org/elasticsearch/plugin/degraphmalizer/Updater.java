package org.elasticsearch.plugin.degraphmalizer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class handles Change instances. The class can be configured via elasticsearch.yml (see README.md for
 * more information). The Updater manages a queue of Change objects, executes HTTP requests for these
 * changes and retries changes when HTTP requests fail.
 */
public class Updater implements Runnable {
    private static final ESLogger LOG = Loggers.getLogger(Updater.class);

    private final BlockingQueue<DelayedImpl<Change>> queue = new DelayQueue<DelayedImpl<Change>>();
    private final HttpClient httpClient = new DefaultHttpClient();

    private final String uriScheme;
    private final String uriHost;
    private final int uriPort;
    private final long retryDelayOnFailureInMillis;
    private final String logPath;
    private final int queueLimit;
    private final int maxRetries;

    private final String index;

    private DiskQueue overflowFile;
    private DiskQueue errorFile;
    // Signals if the memory queue is not available and that changes are written to disk.
    private volatile boolean overflowActive = false;

    private boolean shutdownInProgress = false;

    public Updater(String index, String uriScheme, String uriHost, int uriPort, long retryDelayOnFailureInMillis, String logPath, int queueLimit, int maxRetries) {
        this.index = index;
        this.uriScheme = uriScheme;
        this.uriHost = uriHost;
        this.uriPort = uriPort;
        this.retryDelayOnFailureInMillis = retryDelayOnFailureInMillis;
        this.logPath = logPath;
        this.queueLimit = queueLimit;
        this.maxRetries = maxRetries;

        overflowFile = new DiskQueue(logPath + File.pathSeparator + index + "-overflow.log");
        errorFile = new DiskQueue(logPath + File.pathSeparator + index + "-error.log");

        LOG.info("Updater instantiated for index {}. Updates will be sent to {}://{}:{}. Retry delay on failure is {} milliseconds.", index, uriScheme, uriHost, uriPort, retryDelayOnFailureInMillis);
        LOG.info("Updater will overflow in {} after limit of {} has been reached, messages will be retried {} times ", logPath, queueLimit, maxRetries);
    }

    public void start() {
        new Thread(this).start();
    }

    public void shutdown() {
        shutdownInProgress = true;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void flushQueue() {
        queue.clear();
    }

    public void run() {
        try {
            boolean done = false;
            overflowFile.readFromDiskIntoQueue(queue);
            while (!done) {
                if (queue.isEmpty()) {
                    overflowFile.readFromDiskIntoQueue(queue);
                    overflowActive = false;
                }

                final DelayedImpl<Change> delayed = queue.poll(2, TimeUnit.SECONDS);
                if (delayed!=null) {
                    Change change=delayed.thing();
                    perform(change);
                }

                if (shutdownInProgress) {
                    if (!queue.isEmpty()) {
                        overflowFile.writeToDisk(queue, Integer.MAX_VALUE);
                    }
                    done = true;
                }
            }

            httpClient.getConnectionManager().shutdown();
            LOG.info("Updater stopped for index {}.", index);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting!"); // TODO: ??? (DGM-23)
        } catch (Exception e) {
            LOG.error("Updater for index {} stopped with exception: {}", index, e);
        }
    }

    public void add(final Change change) {
        if (!overflowActive) {
            if (queue.size() >= queueLimit) {
                overflowActive = true;
            } else {
                queue.add(DelayedImpl.immediate(change));
            }
        } else {
            try {
                overflowFile.writeToDisk(change);
            } catch (IOException e) {
                LOG.error("Can't write change {} to overflow file {} ",change,overflowFile.name());
            }
        }
        LOG.trace("Received {}", change);
    }


    private void perform(final Change change) {
        LOG.debug("Attempting to perform {}", change);

        final HttpRequestBase request = toRequest(change);

        try {
            final HttpResponse response = httpClient.execute(request);

            if (!isSuccessful(response)) {
                LOG.warn("Request {} {} was not successful. Response status code: {}.", request.getMethod(), request.getURI(), response.getStatusLine().getStatusCode());
                retry(change);
            } else {
                LOG.debug("Change performed: {}", change);
            }

            try {
                EntityUtils.consume(response.getEntity());
            } finally {
                request.releaseConnection();
            }
        } catch (IOException e) {
            LOG.warn("Error executing request {} {}: {}", request.getMethod(), request.getURI(), e.getMessage());
            retry(change);
        }
    }

    private HttpRequestBase toRequest(final Change change) {
        final HttpRequestBase request;

        final Action action = change.action();
        switch (action) {
            case UPDATE:
                request = new HttpGet(buildURI(change));
                break;
            case DELETE:
                request = new HttpDelete(buildURI(change));
                break;
            default:
                throw new RuntimeException("Unknown action " + action + " for " + change + " on index " + index);
        }

        return request;
    }

    private URI buildURI(final Change change) {
        final String type = change.type();
        final String id = change.id();
        final long version = change.version();

        final String path = String.format("/%s/%s/%s/%d", index, type, id, version);

        try {
            return new URIBuilder()
                    .setScheme(uriScheme)
                    .setHost(uriHost)
                    .setPort(uriPort)
                    .setPath(path)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unexpected error building uri for change " + change + " on index " + index, e);
        }
    }

    private boolean isSuccessful(final HttpResponse response) {
        final int statusCode = response.getStatusLine().getStatusCode();
        return statusCode == 200;
    }

    private void retry(final Change change) {
        if (change.retries()<maxRetries) {
            change.retried();
            final DelayedImpl<Change> delayedChange = new DelayedImpl<Change>(change, change.retries()*retryDelayOnFailureInMillis);
            queue.add(delayedChange);
            LOG.debug("Retrying change {} on index {} in {} milliseconds", change, index, retryDelayOnFailureInMillis);
        } else {
            try {
                LOG.warn("Writing failed change {} to error log {}", change,errorFile.name());
                errorFile.writeToDisk(change);
            } catch (IOException e) {
                LOG.warn("Can't write failed change {} to error log {}",change,errorFile.name());
            }
        }
    }


}
