package org.elasticsearch.plugin.degraphmalizer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

/**
 * This class handles GraphChange instances. The class can be configured via elasticsearch.yml (see README.md for
 * more information). The GraphUpdater manages a queue of GraphChange objects, executes HTTP requests for these
 * changes and retries changes when HTTP requests fail.
 */
public class GraphUpdater implements Runnable
{
    private static final ESLogger LOG = Loggers.getLogger(GraphUpdater.class);

    private final BlockingQueue<DelayedImpl<GraphChange>> queue = new DelayQueue<DelayedImpl<GraphChange>>();
    private final HttpClient httpClient = new DefaultHttpClient();

    private final String uriScheme;
    private final String uriHost;
    private final int uriPort;
    private final long retryDelayOnFailureInMillis;

    private String index;

    private boolean shutdownInProgress = false;

    public GraphUpdater(String index, String uriScheme, String uriHost, int uriPort, long retryDelayOnFailureInMillis)
    {
        this.index=index;
        this.uriScheme=uriScheme;
        this.uriHost=uriHost;
        this.uriPort=uriPort;
        this.retryDelayOnFailureInMillis=retryDelayOnFailureInMillis;

        LOG.info("Graph updater instantiated. Updates will be sent to {}://{}:{}. Retry delay on failure is {} milliseconds.", uriScheme, uriHost, uriPort, retryDelayOnFailureInMillis);
    }

    public void start() {
        new Thread(this).start();
    }

    public void shutdown()
    {
        shutdownInProgress = true;
    }

    public int getQueueSize()
    {
        return queue.size();
    }

    public void flushQueue() {
        queue.clear();
    }

    public void run()
    {
        try
        {
            boolean done = false;
            while (!done)
            {
                final GraphChange change = queue.take().thing();
                perform(change);

                if (shutdownInProgress && queue.isEmpty())
                {
                    done = true;
                }
            }

            httpClient.getConnectionManager().shutdown();
            LOG.info("Graph updater stopped.");
        }
        catch (InterruptedException e)
        {
            LOG.warn("Interrupted while waiting!"); // TODO: ??? (DGM-23)
        }
        catch (Exception e)
        {
            LOG.error("Graph updater stopped with exception: {}", e);
        }
    }

    public void add(final GraphChange change)
    {
        queue.add(DelayedImpl.immediate(change));
        LOG.debug("Received {}", change);
    }


    private void perform(final GraphChange change)
    {
        LOG.debug("Attempting to perform {}", change);

        final HttpRequestBase request = toRequest(change);

        try {
            final HttpResponse response = httpClient.execute(request);

            if (!isSuccessful(response)) {
                LOG.warn("Request {} {} was not successful. Response status code: {}.", request.getMethod(), request.getURI(), response.getStatusLine().getStatusCode());
                retry(change); // TODO: retry until infinity? (DGM-23)
            }
            else
            {
                LOG.debug("Graph change performed: {}", change);
            }

            try
            {
                EntityUtils.consume(response.getEntity());
            }
            finally
            {
                request.releaseConnection();
            }
        }
        catch (IOException e)
        {
            LOG.warn("Error executing request {} {}: {}", request.getMethod(), request.getURI(), e.getMessage());
            retry(change); // TODO: retry until infinity? (DGM-23)
        }
    }

    private HttpRequestBase toRequest(final GraphChange change)
    {
        final HttpRequestBase request;

        final GraphAction action = change.action();
        switch (action)
        {
            case UPDATE:
                request = new HttpGet(buildURI(change));
                break;
            case DELETE:
                request = new HttpDelete(buildURI(change));
                break;
            default:
                throw new RuntimeException("Unknown graph action " + action + " for " + change);
        }

        return request;
    }

    private URI buildURI(final GraphChange change)
    {
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
            throw new RuntimeException("Erg onverwacht, maar toch gebeurd", e);
        }
    }

    private boolean isSuccessful(final HttpResponse response)
    {
        final int statusCode = response.getStatusLine().getStatusCode();
        return statusCode == 200;
    }

    private void retry(final GraphChange change)
    {
        final DelayedImpl<GraphChange> delayedChange = new DelayedImpl<GraphChange>(change, retryDelayOnFailureInMillis);
        queue.add(delayedChange);
        LOG.debug("Retrying {} in {} milliseconds", change, retryDelayOnFailureInMillis);
    }
}
