package graphs.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.*;
import degraphmalizr.EdgeID;
import degraphmalizr.ID;
import exceptions.DegraphmalizerException;
import graphs.GraphQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trees.Pair;

import java.util.ArrayList;
import java.util.List;

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
    public final void commitSubgraph(ID id, Subgraph sg) throws DegraphmalizerException
    {
        if(id.version() == 0)
            throw new IllegalArgumentException("Subgraph must have version > 0");

        if(detectSelfLoops(id, sg))
            throw new IllegalArgumentException("Cannot have self-loops in subgraph (ie. some of your edges point to the id your are committing this Subgraph under.)");

        if(detectNonSymbolicTargets(sg))
            throw new IllegalArgumentException("All edges must link to an identified with version==0");

        boolean success = false;
        try
        {
            // create a list of all elements owned by any version of this subgraph
            final Pair<List<Vertex>, List<Edge>> elementsToDelete = findOwnedElements(id);
            List<Vertex> verticesToDelete = elementsToDelete.a;
            List<Edge> edgesToDelete = elementsToDelete.b;

            // do stuff needed for central vertex...
            final Vertex center = createOrUpdateCentralVertex(id, sg);

            // ...and for the edges
            final Pair<List<Vertex>, List<Edge>> nextVersionElts = createOrUpdateEdges(id, sg);

            // now make sure everything we touched is not deleted
            verticesToDelete.remove(center);
            verticesToDelete.removeAll(nextVersionElts.a);
            edgesToDelete.removeAll(nextVersionElts.b);

            List<Vertex> danglingVertices = findDanglingVertices(id, edgesToDelete);
            verticesToDelete.addAll(danglingVertices);

            removeGraphElements(id, verticesToDelete, edgesToDelete);

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

    // TODO it is probably better to ignore all versions in a subgraph (ie. call getSymbolic on all edges.other())
    private boolean detectNonSymbolicTargets(Subgraph sg)
    {
        for(Subgraph.Edge e : sg.edges())
            if(e.other().version() != 0)
                return true;

        return false;
    }

    private boolean detectSelfLoops(ID center, Subgraph sg)
    {
        for(Subgraph.Edge e : sg.edges())
            if(onlyVersionDiffers(center, e.other()))
                return true;

        return false;
    }

    @Override
    public void deleteSubgraph(final ID id) throws DegraphmalizerException {
        boolean success = false;
        try
        {
            // create a list of all elements owned by any version of this subgraph
            final Pair<List<Vertex>, List<Edge>> elementsToDelete = findOwnedElements(id);
            List<Vertex> verticesToDelete = elementsToDelete.a;
            List<Edge> edgesToDelete = elementsToDelete.b;

            List<Vertex> danglingVertices = findDanglingVertices(id, edgesToDelete);
            verticesToDelete.addAll(danglingVertices);

            removeGraphElements(id, verticesToDelete, edgesToDelete);

            // commit changes to graph
            success = true;
        }
        finally
        {
            // commit or rollback if something failed
            graph.stopTransaction(success ? SUCCESS : FAILURE);
        }
    }

    private List<Vertex> findDanglingVertices(ID id, List<Edge> edgesToDelete) {
        final List<Vertex> danglingVertices = new ArrayList<Vertex>();

        // handle the edges we can delete: We check if they connect vertices that can be deleted too.
        for(Edge e: edgesToDelete)
        {
            // it is possible that a vertex turned symbolic and we are the last edge pointing to it, then remove it
            final Vertex v = e.getVertex(directionOppositeTo(getEdgeID(om, e), id));
            if(canDeleteVertex(v, id, edgesToDelete))
                danglingVertices.add(v);
        }

        return danglingVertices;
    }

    private void removeGraphElements(ID id, List<Vertex> verticesToDelete, List<Edge> edgesToDelete)
    {
        // Remove the edges
        for (Edge e: edgesToDelete)
            graph.removeEdge(e);

        // Remove the vertices
        for (final Vertex v: verticesToDelete)
        {
            if (canDeleteVertex(v, id, edgesToDelete))
                graph.removeVertex(v);
            else
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

    private Vertex createOrUpdateCentralVertex(ID id, Subgraph sg) throws DegraphmalizerException
    {
        // find vertex, doesn't care about version
        Vertex center = resolveVertex(om, graph, id);

        if (center == null)
        {
            log.trace("Couldn't find central vertex with id {}, create new vertex", id);
            center = createVertex(om, graph, id);
            log.trace("Created central vertex");
        }

        // it is actually fine to commit an older version, version management is completely done by ES
        if (log.isWarnEnabled())
        {
            final ID cid = getID(om, center);
            if (isOlder(id, cid))
                log.warn("Commit version < current version", id, cid);
        }

        setProperties(center, sg.properties());

        //if the 'center' vertex has edges, then the edge id should be updated.
        updateEdgeIds(center, id);

        // update the identifier (to the latest version)
        setID(om, center, id);
        setOwner(om, center, id);

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
    private Pair<List<Vertex>, List<Edge>> createOrUpdateEdges(ID id, Subgraph sg)
    {
        List<Vertex> vertexList = new ArrayList<Vertex>();
        List<Edge> edgeList = new ArrayList<Edge>();

        for (Subgraph.Edge e : sg.edges())
        {
            final EdgeID edgeId = Subgraphs.edgeID(id, e);
            Edge edge = findEdge(om, graph, edgeId);

            if (edge != null)
                //check if this edge belongs to this subgraph.
                edgeConsistencyCheck(id, edgeId, edge);
            else
            {
                final Pair<Edge, Vertex> pair = createEdgeAndVertex(id, edgeId);
                vertexList.add(pair.b);
                edge = pair.a;
            }

            // claim edge
            setOwner(om, edge, id);
            setProperties(edge, e.properties());
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