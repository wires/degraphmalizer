package dgm.exceptions;

/**
 * Nodes were found in the graph with a version that cannot be found in Elasticsearch.
 *
 */
public class SourceMissingException extends DegraphmalizerException
{
    public SourceMissingException()
    {
        super("Source document could not be found in ES");
    }
}
