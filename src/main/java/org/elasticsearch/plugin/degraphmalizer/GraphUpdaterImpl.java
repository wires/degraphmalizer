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
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

public class GraphUpdaterImpl implements GraphUpdater, Runnable
{
    private final BlockingQueue<DelayedImpl<GraphChange>> queue = new DelayQueue<DelayedImpl<GraphChange>>();
    private final HttpClient httpClient = new DefaultHttpClient();

    private final String uriScheme;
    private final String uriHost;
    private final int uriPort;
    private final long delayOnFailureInMillis;

    private boolean shutdownInProgress = false;

    @Inject
    public GraphUpdaterImpl(Settings settings)
    {
        final Settings pluginSettings = settings.getComponentSettings(DegraphmalizerPlugin.class);

        // Please keep this in sync with the documentation in README.md
        this.uriScheme = pluginSettings.get("DegraphmalizerPlugin.degraphmalizerScheme", "http");
        this.uriHost = pluginSettings.get("DegraphmalizerPlugin.degraphmalizerHost", "localhost");
        this.uriPort = pluginSettings.getAsInt("DegraphmalizerPlugin.degraphmalizerPort", 9821);
        this.delayOnFailureInMillis = pluginSettings.getAsLong("DegraphmalizerPlugin.delayOnFailureInMillis", 10000l);

        new Thread(this).start();
    }

    @Override
    public void run()
    {
        try
        {
            boolean done = false;
            while (!done)
            {
                GraphChange change = queue.take().thing();
                perform(change);

                if (shutdownInProgress && queue.isEmpty())
                    done = true;
            }
        }
        catch (InterruptedException e)
        {
            // TODO: ??? (DGM-23)
        }
    }

    @Override
    public void add(final GraphChange change)
    {
        queue.add(DelayedImpl.immediate(change));
    }

    @Override
    public void shutdown()
    {
        shutdownInProgress = true;
    }

    private void perform(final GraphChange change)
    {
        final HttpUriRequest request = toRequest(change);

        try
        {
            final HttpResponse response = httpClient.execute(request);

            if (!isSuccessful(response))
                retry(change); // TODO: retry until infinity? (DGM-23)
        }
        catch (IOException e)
        {
            retry(change); // TODO: retry until infinity? (DGM-23)
        }
    }

    private HttpUriRequest toRequest(final GraphChange change)
    {
        final HttpUriRequest request;

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
                throw new RuntimeException("Unknown graph action: " + action);
        }

        return request;
    }

    private URI buildURI(final GraphChange change)
    {
        final String index = change.index();
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
        final DelayedImpl<GraphChange> delayedChange = new DelayedImpl<GraphChange>(change, delayOnFailureInMillis);
        queue.add(delayedChange);
    }
}
