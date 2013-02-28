package dgm.exceptions;

import dgm.degraphmalizr.ID;

/**
 * Nodes were found in the graph with a version that cannot be found in Elasticsearch.
 *
 */
public class NotFoundInGraphException extends DegraphmalizerException
{
    final protected ID id;

    public NotFoundInGraphException(ID id)
    {
        super("Could not find vertex for " + id);
        this.id = id;
    }

    /**
     * Get the ID that couldn't be found.
     */
    public ID id()
    {
        return id;
    }
}
