package graphs.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.*;
import degraphmalizr.EdgeID;
import degraphmalizr.ID;
import exceptions.DegraphmalizerException;
import graphs.GraphQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trees.Pair;

import java.util.*;

import static com.tinkerpop.blueprints.TransactionalGraph.Conclusion.*;
import static graphs.GraphQueries.*;

public class BlueprintsSubgraphManager implements SubgraphManager
{
    private final Logger log = LoggerFactory.getLogger(BlueprintsSubgraphManager.class);

    private final ObjectMapper om;
    private final TransactionalGraph graph;

    public BlueprintsSubgraphManager(ObjectMapper om, TransactionalGraph graph)
    {
        this.graph = graph;
        this.om = om;
    }

    @Override
    public final Subgraph createSubgraph(ID id) throws DegraphmalizerException
    {
        if(id.version() == 0)
            throw new IllegalArgumentException("Subgraph must have version > 0");
        return new SG(id);
    }

    @Override
    public final void commitSubgraph(Subgraph subgraph) throws DegraphmalizerException
    {
        final SG sg = (SG) subgraph;
        boolean success = false;
        try
        {
            // create a list of all elements owned by any version of this subgraph
            final Pair<List<Vertex>, List<Edge>> elementsToDelete = findOwnedElements(sg.id());
            List<Vertex> verticesToDelete = elementsToDelete.a;
            List<Edge> edgesToDelete = elementsToDelete.b;

            // do stuff needed for central vertex...
            final Vertex center = createOrUpdateCentralVertex(sg);

            // ...and for the edges
            final Pair<List<Vertex>, List<Edge>> nextVersionElts = createOrUpdateEdges(sg);

            // now make sure everything we touched is not deleted
            verticesToDelete.remove(center);
            verticesToDelete.removeAll(nextVersionElts.a);
            edgesToDelete.removeAll(nextVersionElts.b);

            List<Vertex> danglingVertices = findDanglingVertices(sg, edgesToDelete);
            verticesToDelete.addAll(danglingVertices);

            removeGraphElements(sg, verticesToDelete, edgesToDelete);

            // commit changes to graph
            success = true;
            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        }
        finally
        {
            // rollback if something failed
            if(! success)
                graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
        }
    }

    @Override
    public void deleteSubgraph(Subgraph subgraph) throws DegraphmalizerException {
        final SG sg = (SG) subgraph;
        boolean success = false;
        try
        {
            // create a list of all elements owned by any version of this subgraph
            final Pair<List<Vertex>, List<Edge>> elementsToDelete = findOwnedElements(sg.id());
            List<Vertex> verticesToDelete = elementsToDelete.a;
            List<Edge> edgesToDelete = elementsToDelete.b;

            List<Vertex> danglingVertices = findDanglingVertices(sg, edgesToDelete);
            verticesToDelete.addAll(danglingVertices);

            removeGraphElements(sg, verticesToDelete, edgesToDelete);

            // commit changes to graph
            success = true;
        }
        finally
        {
            // commit or rollback if something failed
            graph.stopTransaction(success ? SUCCESS : FAILURE);
        }
    }

    private List<Vertex> findDanglingVertices(SG sg, List<Edge> edgesToDelete) {
        final List<Vertex> danglingVertices = new ArrayList<Vertex>();

        // handle the edges we can delete: We check if they connect vertices that can be deleted too.
        for(Edge e: edgesToDelete)
        {
            // it is possible that a vertex turned symbolic and we are the last edge pointing to it, then remove it
            final Vertex v = e.getVertex(directionOppositeTo(getEdgeID(om, e), sg.id()));
            if(canDeleteVertex(v, sg.id(), edgesToDelete))
                danglingVertices.add(v);
        }

        return danglingVertices;
    }

    private void removeGraphElements(SG sg, List<Vertex> verticesToDelete, List<Edge> edgesToDelete) {
        // Remove the edges
        for (Edge e: edgesToDelete)
            graph.removeEdge(e);

        // Remove the vertices
        for (final Vertex v: verticesToDelete)
            if (canDeleteVertex(v, sg.id(), edgesToDelete))
            {
                graph.removeVertex(v);
            }
            else
            {
                GraphQueries.makeSymbolic(om, v);
            }
    }

    private Pair<List<Vertex>, List<Edge>> findOwnedElements(ID id) {
        // create a list of all elements owned by any version of this subgraph
        final List<Vertex> vertexList = new ArrayList<Vertex>();
        for (Vertex v : findOwnedVertices(om, graph, id))
            vertexList.add(v);

        final List<Edge> edgeList = new ArrayList<Edge>();
        for (Edge e : findOwnedEdges(om, graph, id))
            edgeList.add(e);

        return new Pair<List<Vertex>, List<Edge>>(vertexList, edgeList);
    }

    /**
     * A vertex can only be deleted if all edges pointed to it are owned by us and are about to be deleted also.
     * Unless the vertex is not symbolic
     */
    private boolean canDeleteVertex(Vertex v, ID owner, List<Edge> edgesToDelete)
    {
        for(Edge e: v.getEdges(Direction.BOTH))
        {
            // if there is one edge pointing to this vertex that isn't ours, we must keep the vertex
            if(!onlyVersionDiffers(getOwner(om, e), owner))
                return false;

            // and all the other edges must be marked for deletion too!
            if(!edgesToDelete.contains(e))
                return false;

            //if this vertex is not symbolic and not owned by this subgraph, we don't delete it.
            if(! isSymbolic(om, v)) return false;
        }

        // only then can this vertex be removed
        return true;
    }

    private Vertex createOrUpdateCentralVertex(SG sg) throws DegraphmalizerException
    {
        // find vertex, doesn't care about version
        Vertex center = resolveVertex(om, graph, sg.id());

        if (center == null)
        {
            log.trace("Couldn't find central vertex with id {}, create new vertex", sg.id());
            center = createVertex(om, graph, sg.id());
            log.trace("Created central vertex");
        }

        // it is actually fine to commit an older version, version management is completely done by ES
        if (log.isWarnEnabled())
        {
            final ID cid = getID(om, center);
            if (isOlder(sg.id(), cid))
                log.warn("Commit version < current version", sg.id(), cid);
        }

        setProperties(center, sg.properties());

        //if the 'center' vertex has edges, then the edge id should be updated.
        updateEdgeIds(center, sg.id());

        // update the identifier (to the latest version)
        setID(om, center, sg.id());
        setOwner(om, center, sg.id());

        return center;
    }

    private void updateEdgeIds(Vertex center, ID id) throws DegraphmalizerException
    {
        for(Edge edge: center.getEdges(Direction.BOTH))
            setEdgeId(om, updateEdgeId(edge, id), edge);
    }

    private EdgeID updateEdgeId(Edge edge, ID id) throws DegraphmalizerException
    {
        final EdgeID edgeID = getEdgeID(om, edge);

        if (onlyVersionDiffers(edgeID.head(), id))
            return new EdgeID(edgeID.tail(), edgeID.label(), id);

        if (onlyVersionDiffers(edgeID.tail(), id))
            return new EdgeID(id, edgeID.label(), edgeID.head());

        throw new IllegalArgumentException("id:" + id + " is not part of edge id:" + edgeID);
    }

    /**
     * This method iterates over the edges declared in the subgraph.
     * if The edge does not exist in the graph yet, create the target (symbolic) Vertex and the edge.
     * If it does exist check if the existing is owned by this subgraph. if not so: Error Error Error!!!
     * If so: update the properties of that edge.
     * @return A pair of lists that contain all Edges and All vertices that will be part of the new subgraph.
     */
    private Pair<List<Vertex>, List<Edge>> createOrUpdateEdges(SG sg)
    {
        List<Vertex> vertexList = new ArrayList<Vertex>();
        List<Edge> edgeList = new ArrayList<Edge>();

        for (Map.Entry<EdgeID, Map<String, JsonNode>> e : sg.edges().entrySet())
        {
            final EdgeID edgeId = e.getKey();
            Edge edge = findEdge(om, graph, edgeId);

            if (edge != null)
                //check if this edge belongs to this subgraph.
                edgeConsistencyCheck(sg.id(), edgeId, edge);
            else
            {
                final Pair<Edge, Vertex> pair = createEdgeAndVertex(sg.id(), edgeId);
                vertexList.add(pair.b);
                edge = pair.a;
            }

            // claim edge
            setOwner(om, edge, sg.id());
            setProperties(edge, e.getValue());
            edgeList.add(edge);
        }
        return new Pair<List<Vertex>, List<Edge>>(vertexList, edgeList);
    }

    private Pair<Edge, Vertex> createEdgeAndVertex(ID centralVertex, EdgeID edgeId)
    {
        final ID other = getOppositeId(edgeId, centralVertex);

        // we either resolve the symbolic vertex, or create one to represent it
        Vertex v = resolveVertex(om, graph, other);
        if (v == null)
            v = createVertex(om, graph, other);

        final Edge edge = createEdge(om, graph, createOppositeId(edgeId, centralVertex, getID(om, v)));
        return new Pair<Edge,Vertex>(edge, v);
    }


    /**
     * Check if a given edge belongs to this subgraph
     *
     * TODO: if we don't bother with the version component of the owner we can skip all this.
     */
    private void edgeConsistencyCheck(ID centralVertex, EdgeID edgeId, Edge edge)
    {
        final ID owner = getOwner(om, edge);

        if (!onlyVersionDiffers(owner, centralVertex))
            throw new RuntimeException("Edge " + edgeId + " is already owned by " + owner);

        if (centralVertex.version() < owner.version())
            throw new RuntimeException("Committing an older version of a subgraph is not allowed (old = " + owner + ", new = " + centralVertex + ")");
    }
}

class SG implements Subgraph
{
    private final ID id;

    private final Map<EdgeID, Map<String, JsonNode>> edges = new HashMap<EdgeID, Map<String, JsonNode>>();
    private final Map<String, JsonNode> properties = new IdentityHashMap<String, JsonNode>();

    public SG(ID id)
    {
        this.id = id;
    }

    @Override
    public void addEdge(String label, ID other, Direction direction, Map<String, JsonNode> edgeProperties)
    {
        if (other.version() != 0)
            throw new IllegalArgumentException("Other vertex must be a symbolic vertex (version == 0)");

        switch (direction)
        {
            // edge to us
            case IN:
                edges.put(new EdgeID(other, label, id), edgeProperties);
                return;

            // edge from us
            case OUT:
                edges.put(new EdgeID(id, label, other), edgeProperties);
                return;

            default:
                throw new IllegalArgumentException("Direction can be IN or OUT");
        }
    }

    @Override
    public void setProperty(String key, JsonNode value)
    {
        properties.put(key, value);
    }

    public ID id()
    {
        return id;
    }

    public Map<EdgeID, Map<String, JsonNode>> edges()
    {
        return edges;
    }

    public Map<String, JsonNode> properties()
    {
        return properties;
    }
}