package org.elasticsearch.plugin.degraphmalizer.updater;

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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class handles Change instances. The class can be configured via elasticsearch.yml (see README.md for
 * more information). The Updater manages a queue of Change objects, executes HTTP requests for these
 * changes and retries changes when HTTP requests fail.
 */
public final class Updater implements Runnable {
    private static final ESLogger LOG = Loggers.getLogger(Updater.class);

    private final HttpClient httpClient = new DefaultHttpClient();

    private final String uriScheme;
    private final String uriHost;
    private final int uriPort;
    private final long retryDelayOnFailureInMillis;
    private final int maxRetries;

    private final String index;

    private File errorFile;

    private UpdaterQueue queue;
    private boolean shutdownInProgress = false;

    public Updater(final String index, final String uriScheme, final String uriHost, final int uriPort, final long retryDelayOnFailureInMillis, final String logPath, final int queueLimit, final int maxRetries) {
        this.index = index;
        this.uriScheme = uriScheme;
        this.uriHost = uriHost;
        this.uriPort = uriPort;
        this.retryDelayOnFailureInMillis = retryDelayOnFailureInMillis;
        this.maxRetries = maxRetries;

        queue = new UpdaterQueue(logPath, index, queueLimit);
        new Thread(queue).start();

        errorFile = new File(logPath, index + "-error.log");

        LOG.info("Updater instantiated for index {}. Updates will be sent to {}://{}:{}. Retry delay on failure is {} milliseconds.", index, uriScheme, uriHost, uriPort, retryDelayOnFailureInMillis);
        LOG.info("Updater will overflow in {} after limit of {} has been reached, messages will be retried {} times ", logPath, queueLimit, maxRetries);
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
            while (!done) {

                final Change change = queue.take().thing();
                perform(change);

                if (shutdownInProgress) {
                    queue.shutdown();
                    done = true;
                }
            }
        } catch (Exception e) {
            LOG.error("Updater for index {} stopped with exception: {}", index, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
            queue.shutdown();
            LOG.info("Updater stopped for index {}.", index);
        }
    }

    public void add(final Change change) {
        queue.add(DelayedImpl.immediate(change));
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

            EntityUtils.consume(response.getEntity());
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
        if (change.retries() < maxRetries) {
            change.retried();
            final DelayedImpl<Change> delayedChange = new DelayedImpl<Change>(change, change.retries()*retryDelayOnFailureInMillis);
            queue.add(delayedChange);
            LOG.debug("Retrying change {} on index {} in {} milliseconds", change, index, retryDelayOnFailureInMillis);
        } else {
            logError(change);
        }
    }

    public void logError(Change change) {
        try {
            LOG.warn("Writing failed change {} to error log {}", change, errorFile.getCanonicalPath());
            final PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(errorFile, true)));
            writer.println(Change.toValue(change));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            LOG.error("I/O error: " + e.getMessage());
        }
    }
}
