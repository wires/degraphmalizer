package graphs.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.tinkerpop.blueprints.*;
import degraphmalizr.EdgeID;
import degraphmalizr.ID;
import exceptions.DegraphmalizerException;
import trees.Pair;

import java.util.*;

import static graphs.GraphQueries.*;

public class BlueprintsSubgraphManager implements SubgraphManager
{
    private final TransactionalGraph graph;

    public BlueprintsSubgraphManager(TransactionalGraph graph)
    {
        this.graph = graph;
    }

    @Override
    public final Subgraph createSubgraph(ID id) throws DegraphmalizerException
    {
        if(id.version() == 0)
            throw new DegraphmalizerException("Subgraph must have version > 0");
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
            final List<Vertex> verticesToDelete = new ArrayList<Vertex>();
            for (Vertex v : findOwnedVertices(graph, sg.id()))
                verticesToDelete.add(v);

            final List<Edge> edgesToDelete = new ArrayList<Edge>();
            for (Edge e : findOwnedEdges(graph, sg.id()))
                edgesToDelete.add(e);

            // do stuff needed for central vertex...
            final Vertex center = createOrUpdateCentralVertex(sg);

            // ...and for the edges
            final Pair<List<Vertex>, List<Edge>> nextVersionElts = createOrUpdateEdges(sg);

            // now make sure everything we touched is not deleted
            verticesToDelete.remove(center);
            verticesToDelete.removeAll(nextVersionElts.a);
            edgesToDelete.removeAll(nextVersionElts.b);

            // handle the edges we can delete: We check if they connect vertices that can be deleted too.
            for(Edge e: edgesToDelete)
            {
                // it is possible that a vertex turned symbolic and we are the last edge pointing to it, then remove it
                final Vertex v = e.getVertex(directionOppositeTo(getEdgeID(e), sg.id()));
                if(canDeleteVertex(v, sg.id(), edgesToDelete))
                    verticesToDelete.add(v);

                graph.removeEdge(e);
            }

            // Now remove the vertices.
            for(final Vertex v: verticesToDelete)
                if (canDeleteVertex(v, sg.id(), edgesToDelete))
                    graph.removeVertex(v);


            // commit changes to graph
            success = true;
            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        }
        catch(DegraphmalizerException e)
        {
            throw e;
        }
        finally
        {
            // rollback if something failed
            if(! success)
                graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
        }
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
            if(!onlyVersionDiffers(getOwner(e), owner))
                return false;

            // and all the other edges must be marked for deletion too!
            if(!edgesToDelete.contains(e))
                return false;

            //if this vertex is not symbolic and not owned by this subgraph, we don't delete it.
            if(! isSymbolic(v)) return false;
        }

        // only then can this vertex be removed
        return true;
    }

    private Vertex createOrUpdateCentralVertex(SG sg) throws DegraphmalizerException
    {
        // find vertex, doesn't care about version
        Vertex center = resolveVertex(graph, sg.id());

        if (center == null)
           center = createVertex(graph, sg.id());

        if (isOlder(sg.id(), getID(center)))
            throw new DegraphmalizerException("Cannot override older vertex");

        if (! isOwnable(center, sg.id()))
            throw new DegraphmalizerException("It is not possible to change the owner of a vertex");

        setProperties(center, sg.properties);

        //if the 'center' vertex has edges, then the edge id should be updated.
        updateEdgeIds(center, sg.id());

        // update the identifier (to the latest version)
        setID(center, sg.id());
        setOwner(center, sg.id());

        return center;
    }

    private void updateEdgeIds(Vertex center, ID id) throws DegraphmalizerException {
        for(Edge edge: center.getEdges(Direction.BOTH)) {
            updateEdgeId(edge, id);
        }
    }

    private void updateEdgeId(Edge edge, ID id) throws DegraphmalizerException {
        EdgeID edgeID = getEdgeID(edge), newEdgeId;

        if (onlyVersionDiffers(edgeID.head(), id)) {
            newEdgeId = new EdgeID(edgeID.tail(), edgeID.label(), id);
        }else if (onlyVersionDiffers(edgeID.tail(), id)) {
            newEdgeId = new EdgeID(id, edgeID.label(), edgeID.head());
        }else {
            throw new DegraphmalizerException("id:" + id + " is not part of edge id:" + edgeID);
        }
        setEdgeId(newEdgeId, edge);
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

        for (Map.Entry<EdgeID, Map<String, JsonNode>> e : sg.edges.entrySet())
        {
            final EdgeID edgeId = e.getKey();
            Edge edge = findEdge(graph, edgeId);

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
            setOwner(edge, sg.id());
            setProperties(edge, e.getValue());
            edgeList.add(edge);
        }
        return new Pair<List<Vertex>, List<Edge>>(vertexList, edgeList);
    }

    private Pair<Edge, Vertex> createEdgeAndVertex(ID centralVertex, EdgeID edgeId)
    {
        final ID other = getOppositeId(edgeId, centralVertex);

        // we either resolve the symbolic vertex, or create one to represent it
        Vertex v = resolveVertex(graph, other);
        if (v == null)
            v = createVertex(graph, other);

        final Edge edge = createEdge(graph, createOppositeId(edgeId, centralVertex, getID(v)));
        return new Pair<Edge,Vertex>(edge, v);
    }


    /**
     * Check if a given edge belongs to this subgraph
     *
     * TODO: if we don't bother with the version component of the owner we can skip all this.
     */
    private void edgeConsistencyCheck(ID centralVertex, EdgeID edgeId, Edge edge)
    {
        final ID owner = getOwner(edge);

        if (!onlyVersionDiffers(owner, centralVertex))
            throw new RuntimeException("Edge " + edgeId + " is already owned by " + owner);

        if (centralVertex.version() < owner.version())
            throw new RuntimeException("Committing an older version of a subgraph is not allowed (old = " + owner + ", new = " + centralVertex + ")");
    }
}

class SG implements Subgraph
{
    private final ID id;

    final Map<EdgeID, Map<String, JsonNode>> edges = new HashMap<EdgeID, Map<String, JsonNode>>();
    final Map<String, JsonNode> properties = new IdentityHashMap<String, JsonNode>();

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
}