package org.elasticsearch.plugin.degraphmalizer;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesLifecycle;
import org.elasticsearch.indices.IndicesService;

public class DegraphmalizerLifecycleListener extends IndicesLifecycle.Listener
{
    private Map<String, DegraphmalizerIndexListener> listeners = new HashMap<String, DegraphmalizerIndexListener>();
    private DegraphmalizerIndexListenerFactory degraphmalizerIndexListenerFactory;
    private GraphUpdater graphUpdater;

    @Inject
    public DegraphmalizerLifecycleListener(IndicesService indicesService, DegraphmalizerIndexListenerFactory degraphmalizerIndexListenerFactory, GraphUpdater graphUpdater)
    {
        this.degraphmalizerIndexListenerFactory = degraphmalizerIndexListenerFactory;
        this.graphUpdater = graphUpdater;

        indicesService.indicesLifecycle().addListener(this);
    }

    @Override
    public void afterIndexShardStarted(IndexShard indexShard)
    {
        if (indexShard.routingEntry().primary())
        {
            String indexName = getIndexName(indexShard);
            DegraphmalizerIndexListener listener = degraphmalizerIndexListenerFactory.create(indexName);
            listeners.put(indexName, listener);
            indexShard.indexingService().addListener(listener);
        }
    }

    @Override
    public void beforeIndexShardClosed(ShardId shardId, IndexShard indexShard, boolean delete)
    {
        if (indexShard.routingEntry().primary())
        {
            String indexName = getIndexName(indexShard);
            indexShard.indexingService().removeListener(listeners.get(indexName));
            listeners.remove(indexName);
        }

        if (listeners.isEmpty())
        {
            graphUpdater.shutdown();
        }
    }

    private String getIndexName(IndexShard indexShard)
    {
        return indexShard.shardId().index().name();
    }
}
