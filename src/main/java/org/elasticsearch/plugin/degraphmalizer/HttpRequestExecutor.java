package org.elasticsearch.plugin.degraphmalizer;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpRequestExecutor implements Runnable
{
    // TODO: Make this configurable? (DGM-23)
    private final static long FAILURE_DELAY_IN_MILLIS = 10000;

    private final BlockingQueue<DelayedImpl<HttpUriRequest>> queue = new DelayQueue<DelayedImpl<HttpUriRequest>>();
    private final HttpClient httpClient = new DefaultHttpClient();

    @Override
    public void run()
    {
        try
        {
            while (true)
            {
                HttpUriRequest request = queue.take().thing();
                execute(request);
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
    public void add(HttpUriRequest request)
    {
        queue.add(DelayedImpl.immediate(request));
    }

    private void execute(HttpUriRequest request)
    {
        try
        {
            HttpResponse response = httpClient.execute(request);

            if (!isSuccessful(response)) retry(request);
        }
        catch (IOException e)
        {
            retry(request);
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
