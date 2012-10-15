package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesLifecycle;
import org.elasticsearch.indices.IndicesService;


public class DegraphmalizerListener extends IndexingOperationListener
{
    @Override
    public Engine.Create preCreate(Engine.Create create)
    {
        System.err.println("CREATE " + create.docs().size());
        return create;
    }

    @Override
    public Engine.Index preIndex(Engine.Index index)
    {
        System.err.println("INDEX " + index.docs().size());
        return index;
    }

    @Inject
    public DegraphmalizerListener(IndicesService indicesService)
    {
        // listen for changes on the primary shard
        indicesService.indicesLifecycle().addListener(new IndicesLifecycle.Listener() {
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
}
