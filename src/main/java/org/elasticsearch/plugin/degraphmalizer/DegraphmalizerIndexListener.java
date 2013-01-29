package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;

/**
 * This class listens for updates to an index and sends GraphChange objects to the GraphUpdater for processing by the
 * Degraphmalizer.
 */
public class DegraphmalizerIndexListener extends IndexingOperationListener
{
    private GraphUpdaterManager graphUpdaterManager;
    private String index;

    public DegraphmalizerIndexListener(GraphUpdaterManager graphUpdaterManager, String index)
    {
        this.graphUpdaterManager = graphUpdaterManager;
        this.index = index;
    }

    @Override
    public void postCreate(Engine.Create createOperation)
    {
        final String type = createOperation.type();
        final String id = createOperation.id();
        final long version = createOperation.version();

        graphUpdaterManager.add(index, GraphChange.update(type, id, version));
    }

    @Override
    public void postIndex(Engine.Index indexOperation) {
        final String type = indexOperation.type();
        final String id = indexOperation.id();
        final long version = indexOperation.version();

        graphUpdaterManager.add(index, GraphChange.update(type, id, version));
    }

    @Override
    public void postDelete(Engine.Delete deleteOperation)
    {
        final String type = deleteOperation.type();
        final String id = deleteOperation.id();
        final long version = deleteOperation.version();

        graphUpdaterManager.add(index, GraphChange.delete(type, id, version));
    }
}
