package dgm.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import dgm.degraphmalizr.ID;
import dgm.graphs.GraphQueries;
import dgm.trees.Pair;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

/**
 * Retrieve document from elasticsearch, based on Vertex
 *
 * TODO caching
 * TODO query priorities
 *
 * @author wires
 */
public class QueryFunction implements Function<Pair<Edge,Vertex>, Optional<ResolvedPathElement>>
{
    @InjectLogger
    protected Logger log;

    protected final Client searchIndex;
    protected final ObjectMapper objectMapper;

    @Inject
    public QueryFunction(Client searchIndex, ObjectMapper objectMapper)
    {
        this.searchIndex = searchIndex;
        this.objectMapper = objectMapper;
    }

    @Override
    public final Optional<ResolvedPathElement> apply(final Pair<Edge,Vertex> pair)
    {
        // dump information on the current vertex
        if (log.isTraceEnabled())
        {
            log.trace("Retrieving document from ES for vertex {}", pair.b);
            for (String key : pair.b.getPropertyKeys())
                log.trace("Property {} has value '{}'", key, pair.b.getProperty(key));
        }

        // retrieve id property
        final ID id = GraphQueries.getID(objectMapper, pair.b);

        // vertices without ID's cannot be looked up
        if(id == null)
        {
            log.debug("Vertex has no ID assigned, properties are: ");
            for (String key : pair.b.getPropertyKeys())
                log.debug("Property {} has value '{}'", key, pair.b.getProperty(key));

            return Optional.of(new ResolvedPathElement(Optional.<GetResponse>absent(), pair.a, pair.b));
        }

        // this document should not exist in elastic search, otherwise the vertex would have a version > 0
        if(id.version() == 0)
        {
            log.debug("Document {} is symbolic, so we won't attempt to find it in elasticsearch", id);
            return Optional.of(new ResolvedPathElement(Optional.<GetResponse>absent(), pair.a, pair.b));
        }

        // query ES for the document
        final GetResponse r = searchIndex.prepareGet(id.index(), id.type(), id.id())
                .execute().actionGet();

        if ((r.version() == -1) || !r.exists())
        {
            log.debug("Document {} does not exist!", id);
            return Optional.of(new ResolvedPathElement(Optional.<GetResponse>absent(), pair.a, pair.b));
        }

        // query has expired in the meantime
        if (r.version() != id.version())
        {
            log.warn("Document {} expired, version in ES is {}!", id, r.version());
            return Optional.absent();
        }

        return Optional.of(new ResolvedPathElement(Optional.of(r), pair.a, pair.b));
    }
}
