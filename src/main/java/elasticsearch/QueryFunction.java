package elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import degraphmalizr.ID;
import graphs.GraphQueries;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import trees.Pair;

/**
 * Retrieve document from elasticsearch, based on Vertex
 *
 * TODO caching
 * TODO query priorities
 * TODO optional.absent is now considered a query problem, in the future we want to handle each
 *      case separately, see DGM-45
 *
 * @author wires
 */
public class QueryFunction implements Function<Pair<Edge,Vertex>,Optional<GetResponse>>
{
    final protected Logger log;
    final protected Client searchIndex;
    final protected ObjectMapper objectMapper;

    @Inject
    public QueryFunction(Logger log, Client searchIndex, ObjectMapper objectMapper)
    {
        this.log = log;
        this.searchIndex = searchIndex;
        this.objectMapper = objectMapper;
    }

    public Optional<GetResponse> apply(final Pair<Edge,Vertex> pair)
    {
        // dump information on the current vertex
        if (log.isTraceEnabled())
        {
            log.trace("Retrieving document from ES for vertex {}", pair.b);
            for (String key : pair.b.getPropertyKeys())
                log.trace("Property {} has value '{}'", key, pair.b.getProperty(key));
        }

        // retrieve id property
        final ID id = GraphQueries.getID(pair.b);

        // vertices without ID's cannot be looked up
        if(id == null)
        {
            log.debug("Vertex has no ID assigned, properties are: ");
            for (String key : pair.b.getPropertyKeys())
                log.debug("Property {} has value '{}'", key, pair.b.getProperty(key));

            // TODO optional.absent is now considered a query problem, in the future we want to handle each case
            // separately, see DGM-45
            return Optional.absent();
        }

        try
        {
            // query ES for the document
            final GetResponse r = searchIndex.prepareGet(id.index(), id.type(), id.id())
                    .execute().actionGet();

            // query has expired in the meantime
            if (r.version() != id.version())
            {
                log.debug("Document {} expired, version in ES is {}!", id, r.version());
                // TODO optional.absent is now considered a query problem, in the future we want to handle each case
                // separately, see DGM-45
                return Optional.absent();
            }

            if (!r.exists())
            {
                log.debug("Document {} does not exist!", id);
                // TODO optional.absent is now considered a query problem, in the future we want to handle each case
                // separately, see DGM-45
                return Optional.of(r);
            }

            return Optional.of(r);
        }
        catch (ElasticSearchException e)
        {
            return Optional.absent();
        }
    }
}
