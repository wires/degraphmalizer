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

/**
 * This class is responsible for starting and stopping DegraphmalizerIndexShardListener instances for all primary index
 * shards.
 */
public class DegraphmalizerLifecycleListener extends IndicesLifecycle.Listener
{
    private static final ESLogger LOG = Loggers.getLogger(DegraphmalizerLifecycleListener.class);

    private final Map<ShardId, DegraphmalizerIndexShardListener> listeners = new HashMap<ShardId, DegraphmalizerIndexShardListener>();
    private final GraphUpdaterManager graphUpdaterManager;

    @Inject
    public DegraphmalizerLifecycleListener(IndicesService indicesService, GraphUpdaterManager graphUpdaterManager)
    {
        this.graphUpdaterManager = graphUpdaterManager;

        indicesService.indicesLifecycle().addListener(this);
    }

    public void afterIndexCreated(IndexService indexService) {
        String indexName=indexService.index().name();
        if (isRelevantForDegraphmalizer(indexName)) {
            graphUpdaterManager.startGraphUpdater(indexName);
            LOG.info("Graphupdater started for index {}",indexName);
        }
    }

    public void afterIndexClosed(Index index, boolean delete) {
        String indexName=index.name();
        if (isRelevantForDegraphmalizer(indexName)) {
            graphUpdaterManager.stopGraphUpdater(indexName);
            LOG.info("Graphupdater stopped for index {}",indexName);
        }
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

        final DegraphmalizerIndexShardListener shardListener = new DegraphmalizerIndexShardListener(graphUpdaterManager, indexName);
        final ShardId shardId = indexShard.shardId();
        listeners.put(shardId, shardListener);
        indexShard.indexingService().addListener(shardListener);

        LOG.info("Index shard listener added for shard {}", shardId);
    }

    private void removeIndexShardListener(ShardId shardId, IndexShard indexShard)
    {
        final String indexName = getIndexName(indexShard);
        final DegraphmalizerIndexShardListener shardListener = listeners.get(shardId);
        indexShard.indexingService().removeListener(shardListener);
        listeners.remove(shardId);

        LOG.info("Index shard listener removed for shard {}", shardId);
    }

    private boolean isRelevantForDegraphmalizer(String index)
    {
        // Don't add index listeners for 'private' indices
        return !index.startsWith("_");
    }

    private boolean isRelevantForDegraphmalizer(IndexShard indexShard) {
        return isRelevantForDegraphmalizer(getIndexName(indexShard));
    }

    private String getIndexName(IndexShard indexShard)
    {
        return indexShard.shardId().index().name();
    }
}
