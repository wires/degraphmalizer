package org.elasticsearch.plugin.degraphmalizer;

import org.apache.http.client.methods.HttpUriRequest;

public interface HttpRequestExecutor {

    public void add(HttpUriRequest request);

    public void shutdown();
}
