package dgm.exceptions;

/**
 * Nodes were found in the graph with a version that cannot be found in Elasticsearch.
 *
 */
public class DocumentFiltered extends DegraphmalizerException
{
    public DocumentFiltered()
    {
        super("Document was filtered: graph contents extracted, but no target document written", Severity.INFO);
    }
}
