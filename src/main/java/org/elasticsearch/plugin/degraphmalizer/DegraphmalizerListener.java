package org.elasticsearch.plugin.degraphmalizer;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesLifecycle;
import org.elasticsearch.indices.IndicesService;

public class DegraphmalizerListener extends IndexingOperationListener
{
    // TODO: load URI_* from a configuration file (DGM-23)
    private static final String URI_SCHEME = "http";
    private static final String URI_HOST   = "localhost";
    private static final int    URI_PORT   = 9200;

    @Inject
    public DegraphmalizerListener(IndicesService indicesService)
    {
        // listen for changes on the primary shard
        indicesService.indicesLifecycle().addListener(new IndicesLifecycle.Listener()
        {
            @Override
            public void afterIndexShardStarted(IndexShard indexShard)
            {
                if(!indexShard.routingEntry().primary())
                    return;

                indexShard.indexingService().addListener(DegraphmalizerListener.this);
            }

            @Override
            public void beforeIndexShardClosed(ShardId shardId, IndexShard indexShard, boolean delete)
            {
                if(!indexShard.routingEntry().primary())
                    return;

                indexShard.indexingService().addListener(DegraphmalizerListener.this);
            }
        });
    }

    @Override
    public Engine.Create preCreate(Engine.Create create)
    {
        System.err.println("CREATE " + create.docs().size());
        return create;
    }

    @Override
    public Engine.Index preIndex(Engine.Index index)
    {
        updateGraph(index);
        return index;
    }

    private void updateGraph(Engine.Index index)
    {
        try
        {
            final String indexName = "???";       // TODO: Where do we get this? (DGM-23)
            final String type = index.type();     // TODO: Is this the type we're looking for? (DGM-23)
            final String id = index.id();         // TODO: Is this the id we're looking for? (DGM-23)
            final long version = index.version(); // TODO: Is this the version we're looking for? (DGM-23)

            final String path = String.format("/%s/%s/%s/%d", indexName, type, id, version);

            final URI uri = new URIBuilder()
                    .setScheme(URI_SCHEME)
                    .setHost(URI_HOST)
                    .setPort(URI_PORT)
                    .setPath(path)
                    .build();

            final HttpGet httpGet = new HttpGet(uri);

            queue(httpGet);
        }
        catch (URISyntaxException e)
        {
            // TODO: ??? (DGM-23)
        }
    }

    private void queue(HttpUriRequest request)
    {
        // TODO: get a reference to the HttpRequestExecutor instance and queue the request (DGM-23)
        // httpRequestExecutor.add(request);
    }
}
