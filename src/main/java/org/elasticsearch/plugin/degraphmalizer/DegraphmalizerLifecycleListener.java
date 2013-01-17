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
    private static final ESLogger LOG = Loggers.getLogger(DegraphmalizerLifecycleListener.class);

    private final Map<ShardId, DegraphmalizerIndexListener> listeners = new HashMap<ShardId, DegraphmalizerIndexListener>();
    private final GraphUpdater graphUpdater;

    @Inject
    public DegraphmalizerLifecycleListener(IndicesService indicesService, GraphUpdater graphUpdater)
    {
        this.graphUpdater = graphUpdater;

        indicesService.indicesLifecycle().addListener(this);
    }

    @Override
    public void afterIndexShardStarted(IndexShard indexShard)
    {
        if (!indexShard.routingEntry().primary())
            return;

        final String indexName = getIndexName(indexShard);
        final DegraphmalizerIndexListener listener = new DegraphmalizerIndexListener(graphUpdater, indexName);
        final ShardId shardId = indexShard.shardId();
        listeners.put(shardId, listener);
        indexShard.indexingService().addListener(listener);

        LOG.info("Index listener added for shard {}", shardId);
    }

    @Override
    public void beforeIndexShardClosed(ShardId shardId, IndexShard indexShard, boolean delete)
    {
        if (!indexShard.routingEntry().primary())
            return;

        final DegraphmalizerIndexListener listener = listeners.get(shardId);
        indexShard.indexingService().removeListener(listener);
        listeners.remove(shardId);

        LOG.info("Index listener removed for shard {}", shardId);
    }

    private String getIndexName(IndexShard indexShard)
    {
        return indexShard.shardId().index().name();
    }
}
