package dgm.graphs.ops;

import com.fasterxml.jackson.databind.JsonNode;
import dgm.degraphmalizr.EdgeID;
import dgm.degraphmalizr.ID;
import dgm.exceptions.UnreachableCodeReachedException;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class EmptySubgraph implements Subgraph
{
    @Override
    public Iterable<Edge> edges()
    {
        return Iterables.empty();
    }

    @Override
    public Map<String, JsonNode> properties()
    {
        return Collections.emptyMap();
    }
}


class MergedSubgraph implements Subgraph
{
    final ArrayList<Subgraph.Edge> edges = new ArrayList<Subgraph.Edge>();
    final Map<String,JsonNode> properties = new HashMap<String, JsonNode>();

    final Logger log = LoggerFactory.getLogger(MergedSubgraph.class);

    public MergedSubgraph(Iterable<Subgraph> subgraphs)
    {
        for(Subgraph s : subgraphs)
        {
            // add all edges
            for(Subgraph.Edge se : s.edges())
            {
                // check and warn for duplicate edges
                hasEdgeCheck(se);
                edges.add(se);
            }

            // add all properties
            for(String key : s.properties().keySet())
                if(properties.containsKey(key))
                    log.warn("Duplicate property with key {} found while merging", key);

            properties.putAll(s.properties());
        }
    }

    private boolean hasEdgeCheck(Subgraph.Edge edge)
    {
        for(Subgraph.Edge e : edges)
        {
            final boolean a = e.other().equals(edge.other());
            final boolean b = e.label().equals(edge.label());
            final boolean c = e.direction().equals(edge.direction());

            if(a && b && c)
            {
                log.warn("Adding an edge ({},{},{}) that already exists in the subgraph",
                        new Object[]{edge.label(), edge.other(), edge.direction()});
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterable<Edge> edges()
    {
        return edges;
    }

    @Override
    public Map<String, JsonNode> properties()
    {
        return properties;
    }
}

/**
 * Helper methods for {@link Subgraph}s
 */
public class Subgraphs
{
    final public static Subgraph EMPTY_SUBGRAPH = new EmptySubgraph();

    /**
     * If the Subgraph if commited with ID {@code center}, compute the {@link EdgeID} for this {@code edge}.
     */
    public static final EdgeID edgeID(final ID center, final Subgraph.Edge edge)
    {

        switch (edge.direction())
        {
            case INWARDS:
                // center <---- edge.other()
                return new EdgeID(edge.other(), edge.label(), center);

            case OUTWARDS:
                // center ----> edge.other()
                return new EdgeID(center, edge.label(), edge.other());

            default:
                throw new UnreachableCodeReachedException();
        }
    }

    public static Subgraph merge(Iterable<Subgraph> subgraphs)
    {
        return new MergedSubgraph(subgraphs);
    }

    public static Subgraph merge(Subgraph... subgraphs)
    {
        return merge(Arrays.asList(subgraphs));
    }
}
