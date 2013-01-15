package org.elasticsearch.plugin.degraphmalizer;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;

public class DegraphmalizerIndexListener extends IndexingOperationListener
{
    // TODO: load URI_* from a configuration file (DGM-23)
    private static final String URI_SCHEME = "http";
    private static final String URI_HOST   = "localhost";
    private static final int    URI_PORT   = 9200;

    private HttpRequestExecutor httpRequestExecutor;
    private String index;

    @Inject
    public DegraphmalizerIndexListener(HttpRequestExecutor httpRequestExecutor, @Assisted String index)
    {
        this.httpRequestExecutor = httpRequestExecutor;
        this.index = index;
    }

    @Override
    public Engine.Create preCreate(Engine.Create createOperation)
    {
        final String type = createOperation.type();
        final String id = createOperation.id();
        final long version = createOperation.version();

        get(type, id, version);

        return createOperation;
    }

    @Override
    public Engine.Index preIndex(Engine.Index indexOperation)
    {
        final String type = indexOperation.type();
        final String id = indexOperation.id();
        final long version = indexOperation.version();

        get(type, id, version);

        return indexOperation;
    }

    @Override
    public Engine.Delete preDelete(Engine.Delete deleteOperation)
    {
        final String type = deleteOperation.type();
        final String id = deleteOperation.id();
        final long version = deleteOperation.version();

        delete(type, id, version);

        return deleteOperation;
    }

    private void get(final String type, final String id, final long version)
    {
        final URI uri = buildURI(type, id, version);
        final HttpGet httpGet = new HttpGet(uri);
        httpRequestExecutor.add(httpGet);
    }

    private void delete(final String type, final String id, final long version)
    {
        final URI uri = buildURI(type, id, version);
        final HttpDelete httpDelete = new HttpDelete(uri);
        httpRequestExecutor.add(httpDelete);
    }

    private URI buildURI(final String type, final String id, final long version)
    {
        final String path = String.format("/%s/%s/%s/%d", index, type, id, version);

        try {
            return new URIBuilder()
                .setScheme(URI_SCHEME)
                .setHost(URI_HOST)
                .setPort(URI_PORT)
                .setPath(path)
                .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Erg onverwacht, maar toch gebeurd", e);
        }
    }
}
