package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;
import org.elasticsearch.plugin.degraphmalizer.updater.Change;
import org.elasticsearch.plugin.degraphmalizer.updater.UpdaterManager;

import static org.elasticsearch.index.engine.Engine.Operation.Origin.PRIMARY;

/**
 * This class listens for updates to an indexshard and sends Change objects to the Updater if the indexshard is primary for processing by the
 * Degraphmalizer.
 */
public class DegraphmalizerIndexShardListener extends IndexingOperationListener
{
    private static final ESLogger LOG = Loggers.getLogger(DegraphmalizerIndexShardListener.class);

    private UpdaterManager graphUpdaterManager;
    private String index;

    public DegraphmalizerIndexShardListener(final UpdaterManager graphUpdaterManager, final String index)
    {
        this.graphUpdaterManager = graphUpdaterManager;
        this.index = index;
    }

    @Override
    public void postCreate(final Engine.Create createOperation)
    {
        final String type = createOperation.type();
        final String id = createOperation.id();
        final long version = createOperation.version();

        LOG.trace("Origin {} of create to id {} ", createOperation.origin(), id);
        if (isFromPrimary(createOperation)) {
            graphUpdaterManager.add(index, Change.update(type, id, version));
        }
    }

    @Override
    public void postIndex(final Engine.Index indexOperation)
    {
        final String type = indexOperation.type();
        final String id = indexOperation.id();
        final long version = indexOperation.version();

        LOG.trace("Origin {} of index to id {} ", indexOperation.origin(), id);
        if (isFromPrimary(indexOperation))
        {
            graphUpdaterManager.add(index, Change.update(type, id, version));
        }
    }

    @Override
    public void postDelete(final Engine.Delete deleteOperation)
    {
        final String type = deleteOperation.type();
        final String id = deleteOperation.id();
        final long version = deleteOperation.version();

        LOG.trace("Origin {} of index to id {} ", deleteOperation.origin(), id);
        if (isFromPrimary(deleteOperation))
        {
            graphUpdaterManager.add(index, Change.delete(type, id, version));
        }
    }

    private boolean isFromPrimary(final Engine.Operation operation)
    {
        return PRIMARY.equals(operation.origin());
    }
}
