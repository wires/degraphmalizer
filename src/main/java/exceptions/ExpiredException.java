package exceptions;

import degraphmalizr.ID;
import scala.actors.threadpool.Arrays;

import java.util.Iterator;

/**
 * Nodes were found in the graph with a version that cannot be found in Elasticsearch.
 *
 */
public class ExpiredException extends DegraphmalizerException
{
    final Iterable<ID> expired;

    public ExpiredException(Iterable<ID> expired)
    {
        super(exceptionMessage(expired));
        this.expired = expired;
    }

    public ExpiredException(ID... expired)
    {
        this(Arrays.asList(expired));
    }

    private static String exceptionMessage(Iterable<ID> expired)
    {
        final StringBuilder sb = new StringBuilder("Query expired for ids: ");

        // id1; id2; id3 ...
        final Iterator<ID> ids = expired.iterator();
        while(ids.hasNext())
        {
            final ID id = ids.next();
            sb.append(id.toString());
            if(ids.hasNext())
                sb.append("; ");
        }

        return sb.toString();
    }

    /**
     * Get a list of expired ID's.
     *
     * This means that the ID in the graph has a version that can not be found in elasticsearch.
     */
    public Iterable<ID> expired()
    {
        return expired;
    }
}
