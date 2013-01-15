package org.elasticsearch.plugin.degraphmalizer;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpRequestExecutorImpl implements Runnable, HttpRequestExecutor
{
    // TODO: Make this configurable? (DGM-23)
    private final static long FAILURE_DELAY_IN_MILLIS = 10000;

    private final BlockingQueue<DelayedImpl<HttpUriRequest>> queue = new DelayQueue<DelayedImpl<HttpUriRequest>>();
    private final HttpClient httpClient = new DefaultHttpClient();

    private boolean shutdownInProgress = false;

    public HttpRequestExecutorImpl()
    {
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
                HttpUriRequest request = queue.take().thing();
                execute(request);

                if (shutdownInProgress && queue.isEmpty())
                    done = true;
            }
        }
        catch (InterruptedException e)
        {
            // TODO: ??? (DGM-23)
        }
    }

    /**
     * Add a request for immediate execution.
     *
     * @param request The request to execute.
     */
    @Override
    public void add(HttpUriRequest request)
    {
        queue.add(DelayedImpl.immediate(request));
    }

    @Override
    public void shutdown() {
        shutdownInProgress = true;
    }

    private void execute(HttpUriRequest request)
    {
        try
        {
            HttpResponse response = httpClient.execute(request);

            if (!isSuccessful(response)) retry(request); // TODO: retry until infinity? (DGM-23)
        }
        catch (IOException e)
        {
            retry(request); // TODO: retry until infinity? (DGM-23)
        }
    }

    private boolean isSuccessful(HttpResponse response)
    {
        final int statusCode = response.getStatusLine().getStatusCode();
        return statusCode == 200;
    }

    private void retry(HttpUriRequest request)
    {
        DelayedImpl<HttpUriRequest> delayedRequest = new DelayedImpl<HttpUriRequest>(request, FAILURE_DELAY_IN_MILLIS);
        queue.add(delayedRequest);
    }
}
