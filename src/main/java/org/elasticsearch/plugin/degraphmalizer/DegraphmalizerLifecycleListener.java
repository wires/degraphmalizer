package org.elasticsearch.plugin.degraphmalizer;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesLifecycle;
import org.elasticsearch.indices.IndicesService;

public class DegraphmalizerLifecycleListener extends IndicesLifecycle.Listener
{
    private static final ESLogger LOGGER = Loggers.getLogger(DegraphmalizerLifecycleListener.class);

    private Map<String, DegraphmalizerIndexListener> listeners = new HashMap<String, DegraphmalizerIndexListener>();
    private GraphUpdater graphUpdater;

    @Inject
    public DegraphmalizerLifecycleListener(IndicesService indicesService, GraphUpdater graphUpdater)
    {
        this.graphUpdater = graphUpdater;

        indicesService.indicesLifecycle().addListener(this);
    }

    @Override
    public void afterIndexShardStarted(IndexShard indexShard)
    {
        if (indexShard.routingEntry().primary())
        {
            String indexName = getIndexName(indexShard);
            DegraphmalizerIndexListener listener = new DegraphmalizerIndexListener(graphUpdater, indexName);
            listeners.put(indexName, listener);
            indexShard.indexingService().addListener(listener);

            LOGGER.info("Index listener added for index {}", indexName);
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

            LOGGER.info("Index listener removed for index {}", indexName);
        }

        if (listeners.isEmpty())
        {
            LOGGER.info("No more index listeners, shutting down the graph updater...");

            graphUpdater.shutdown();
        }
    }

    private String getIndexName(IndexShard indexShard)
    {
        return indexShard.shardId().index().name();
    }
}
