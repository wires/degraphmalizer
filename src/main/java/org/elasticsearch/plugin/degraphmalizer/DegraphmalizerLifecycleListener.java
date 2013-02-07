package org.elasticsearch.plugin.degraphmalizer;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesLifecycle;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.plugin.degraphmalizer.updater.UpdaterManager;

/**
 * This class is responsible for starting and stopping DegraphmalizerIndexShardListener instances.
 */
public class DegraphmalizerLifecycleListener extends IndicesLifecycle.Listener
{
    private static final ESLogger LOG = Loggers.getLogger(DegraphmalizerLifecycleListener.class);

    private final Map<ShardId, DegraphmalizerIndexShardListener> listeners = new HashMap<ShardId, DegraphmalizerIndexShardListener>();
    private final UpdaterManager updaterManager;

    @Inject
    public DegraphmalizerLifecycleListener(IndicesService indicesService, UpdaterManager updaterManager)
    {
        this.updaterManager = updaterManager;

        indicesService.indicesLifecycle().addListener(this);
    }

    @Override
    public void afterIndexCreated(final IndexService indexService) {
        final String indexName = indexService.index().name();
        if (isRelevantForDegraphmalizer(indexName)) {
            updaterManager.startUpdater(indexName);
        }
    }

    @Override
    public void afterIndexClosed(final Index index, final boolean delete) {
        final String indexName = index.name();
        if (isRelevantForDegraphmalizer(indexName)) {
            updaterManager.stopUpdater(indexName);
        }
    }

    @Override
    public void afterIndexShardStarted(final IndexShard indexShard)
    {
        if (isRelevantForDegraphmalizer(indexShard))
        {
            addIndexShardListener(indexShard);
        }
    }

    @Override
    public void beforeIndexShardClosed(final ShardId shardId, final IndexShard indexShard, final boolean delete)
    {
        if (isRelevantForDegraphmalizer(indexShard))
        {
            removeIndexShardListener(shardId, indexShard);
        }
    }

    private void addIndexShardListener(final IndexShard indexShard)
    {
        final String indexName = getIndexName(indexShard);

        final DegraphmalizerIndexShardListener shardListener = new DegraphmalizerIndexShardListener(updaterManager, indexName);
        final ShardId shardId = indexShard.shardId();
        listeners.put(shardId, shardListener);
        indexShard.indexingService().addListener(shardListener);

        LOG.info("Index shard listener added for shard {}", shardId);
    }

    private void removeIndexShardListener(final ShardId shardId, final IndexShard indexShard)
    {
        final DegraphmalizerIndexShardListener shardListener = listeners.get(shardId);
        indexShard.indexingService().removeListener(shardListener);
        listeners.remove(shardId);

        LOG.info("Index shard listener removed for shard {}", shardId);
    }

    private boolean isRelevantForDegraphmalizer(final String index)
    {
        // Don't add index listeners for 'private' indices
        return !index.startsWith("_");
    }

    private boolean isRelevantForDegraphmalizer(final IndexShard indexShard) {
        return isRelevantForDegraphmalizer(getIndexName(indexShard));
    }

    private String getIndexName(final IndexShard indexShard)
    {
        return indexShard.shardId().index().name();
    }
}
