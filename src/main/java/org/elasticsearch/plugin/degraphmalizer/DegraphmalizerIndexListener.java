package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;

/**
 * This class listens for updates to an index and sends GraphChange objects to the GraphUpdater for processing by the
 * Degraphmalizer.
 */
public class DegraphmalizerIndexListener extends IndexingOperationListener
{
    private GraphUpdater graphUpdater;
    private String index;

    @Inject
    public DegraphmalizerIndexListener(GraphUpdater graphUpdater, String index)
    {
        this.graphUpdater = graphUpdater;
        this.index = index;
    }

    @Override
    public Engine.Create preCreate(Engine.Create createOperation)
    {
        final String type = createOperation.type();
        final String id = createOperation.id();
        final long version = createOperation.version();

        graphUpdater.add(GraphChange.update(index, type, id, version));

        return createOperation;
    }

    @Override
    public Engine.Index preIndex(Engine.Index indexOperation)
    {
        final String type = indexOperation.type();
        final String id = indexOperation.id();
        final long version = indexOperation.version();

        graphUpdater.add(GraphChange.update(index, type, id, version));

        return indexOperation;
    }

    @Override
    public Engine.Delete preDelete(Engine.Delete deleteOperation)
    {
        final String type = deleteOperation.type();
        final String id = deleteOperation.id();
        final long version = deleteOperation.version();

        graphUpdater.add(GraphChange.delete(index, type, id, version));

        return deleteOperation;
    }
}
