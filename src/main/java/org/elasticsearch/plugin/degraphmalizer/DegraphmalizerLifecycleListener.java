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

/**
 * This class is responsible for starting and stopping DegraphmalizerIndexListener instances for all primary index
 * shards.
 */
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
        if (isRelevantForDegraphmalizer(indexShard))
        {
            addIndexShardListener(indexShard);
        }
    }

    @Override
    public void beforeIndexShardClosed(ShardId shardId, IndexShard indexShard, boolean delete)
    {
        if (isRelevantForDegraphmalizer(indexShard))
        {
            removeIndexShardListener(shardId, indexShard);
        }
    }

    private void addIndexShardListener(IndexShard indexShard)
    {
        final String indexName = getIndexName(indexShard);
        final DegraphmalizerIndexListener listener = new DegraphmalizerIndexListener(graphUpdater, indexName);
        final ShardId shardId = indexShard.shardId();
        listeners.put(shardId, listener);
        indexShard.indexingService().addListener(listener);

        LOG.info("Index listener added for shard {}", shardId);
    }

    private void removeIndexShardListener(ShardId shardId, IndexShard indexShard)
    {
        final DegraphmalizerIndexListener listener = listeners.get(shardId);
        indexShard.indexingService().removeListener(listener);
        listeners.remove(shardId);

        LOG.info("Index listener removed for shard {}", shardId);
    }

    private boolean isRelevantForDegraphmalizer(IndexShard indexShard)
    {
        // Only add index listeners to primary index shards, to avoid duplicate updates
        if (!indexShard.routingEntry().primary())
            return false;

        final String indexName = getIndexName(indexShard);

        // Don't add index listeners for 'private' indices
        if (indexName.startsWith("_"))
            return false;

        return true;
    }

    private String getIndexName(IndexShard indexShard)
    {
        return indexShard.shardId().index().name();
    }
}
