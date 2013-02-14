package graphs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.*;
import configuration.javascript.JSONUtilities;
import degraphmalizr.EdgeID;
import degraphmalizr.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trees.Pair;
import trees.Tree;

import java.io.IOException;
import java.util.*;

public final class GraphQueries
{
    private static final Logger log = LoggerFactory.getLogger(GraphQueries.class);

    public static final String PREFIX             = "_";
    public static final String IDENTIFIER         = PREFIX + "identifier";
    public static final String SYMBOLIC_IDENTIFER = PREFIX + "symbolic";
    public static final String OWNER              = PREFIX + "owner";
    public static final String SYMBOLIC_OWNER     = PREFIX + "symbolicOwner";

    private GraphQueries() {}

	/**
	 * Compute the vertices reached from <code>s</code> in one step in direction <code>d</code>.
	 *
	 * @param g the graph to operate on
	 * @param s Initial vertex
	 * @param d Direction to follow edges
	 *
	 * @return Tree with value (null, s) and then all children as specified
	 */
    // TODO non-recursive
    public static Tree<Pair<Edge, Vertex>> childrenFrom(Graph g, Vertex s, Direction d)
	{
		return childrenFrom(g, null, s, d, 1);
	}

    // TODO non-recursive
    private static Tree<Pair<Edge, Vertex>> childrenFrom(Graph g, Edge e, Vertex s, Direction d, int depth)
    {
        // TODO handle cycles in the graph
        if (depth > 100)
            throw new RuntimeException("Found a graph with a path longer that 100 nodes, this probably indicated a cyclic walk");

        final ArrayList<Tree<Pair<Edge, Vertex>>> children = new ArrayList<Tree<Pair<Edge, Vertex>>>();

        for(Edge ed : s.getEdges(d))
            children.add(childrenFrom(g, ed, ed.getVertex(d.opposite()), d, depth + 1));

        return new Tree<Pair<Edge,Vertex>>(new Pair<Edge,Vertex>(e, s), children);
    }



    /**
     * Set the property of an element to a json value.
     *
     * If the value is an object the property is set to the JSON string, otherwise the property is set to the native
     * value represented by the JsonNode.
     *
     * <ul>
     * <li>So "'foo'" is stored as a String "foo"
     * <li>A boolean "true" is stored as Boolean.TRUE, etc.
     * <li>But "{'foo':123}" is stored as a String "{'foo':123}"
     * </ul>
     *
     * Normally you would just store (JSON) values, not objects or arrays.
     *
     * @param elt The node or edge of which to set the property
     * @param property Property name
     * @param value Property value
     */
    public static void setProperty(Element elt, String property, JsonNode value)
    {
        // TODO use com.tinkerpop.blueprints.Features , supportsBooleanProperty, etc...
        checkPropertyName(property);

        // values are directly inserted as strings
        if(value.isValueNode())
        {
            elt.setProperty(property, value.asText());
            return;
        }

        // objects are inserted as json strings
        elt.setProperty(property, value.toString());
    }

    public static JsonNode getProperty(ObjectMapper om, Element elt, String property)
    {
        checkPropertyName(property);

        final Object obj = elt.getProperty(property);

        if(! (obj instanceof String))
            throw new RuntimeException("Property " + property + " is not in the expected format (String), it's a "
                    + obj.getClass().getSimpleName());

        final String s = String.valueOf(obj);

        try
        {
            return om.readTree(s);
        }
        catch (IOException z)
        {
            try
            {
                // TODO this is very shady.. anything that doesn't parse as JSON is read as string...
                return om.readTree("\"" + s + "\"");
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to parse property " + property + " from '" + s +"'", z);
            }
        }
    }

    /**
     * This method removes all properties (except the id and owner ones) and sets new properties.
     */
    public static void setProperties(Element element, Map<String, JsonNode> properties)
    {
        for (String key : element.getPropertyKeys())
        {
            if (! key.startsWith(GraphQueries.PREFIX))
                element.removeProperty(key);
        }

        for (Map.Entry<String, JsonNode> e : properties.entrySet())
            setProperty(element, e.getKey(), e.getValue());
    }

    public static void checkPropertyName(String name)
    {
        if(name.equals(IDENTIFIER) || name.equals(OWNER) || name.equals(SYMBOLIC_IDENTIFER) || name.equals(SYMBOLIC_OWNER))
            throw new IllegalArgumentException("Property name '" + name + "' is a reserved name");
    }

    public static Edge findEdge(ObjectMapper om, Graph G, EdgeID edgeID)
    {
        final String id = getStringRepresentation(om, edgeID);
        final Iterator<Edge> ei = G.getEdges(IDENTIFIER, id).iterator();
        if(!ei.hasNext())
            return null;

        final Edge e = ei.next();

        if(ei.hasNext())
            throw new RuntimeException("Graph inconsistency! More than one edge with (head,label,tail) coordinate "
                    + id); // TODO: Consistently handle these inconsistencies

        return e;
    }

    private static String getStringRepresentation(ObjectMapper om, final EdgeID edgeID)
    {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(JSONUtilities.toJSON(om, edgeID.tail()).toString())
                     .append("--")
                     .append(edgeID.label())
                     .append("->")
                     .append(JSONUtilities.toJSON(om, edgeID.head()).toString());
        return stringBuilder.toString();
    }


    private static Vertex findVertexOnProperty(ObjectMapper om, Graph G, ID id, String propertyName)
    {
        final String idStr = JSONUtilities.toJSON(om, id).toString();
        final Iterator<Vertex> vi = G.getVertices(propertyName, idStr).iterator();
        if(!vi.hasNext())
            return null;

        final Vertex v = vi.next();

        if(vi.hasNext())
            throw new RuntimeException("Graph inconsistency! More than one vertex with identifier " + id);

        return v;
    }

    public static Vertex findVertex(ObjectMapper om, Graph G, ID id)
    {
        return findVertexOnProperty(om, G, id, IDENTIFIER);
    }

    /**
     * Find all vertices owner by the specified ID, don't look at versions.
     * This method will not return the ownable symbolic vertices.
     */
    public static Iterable<Vertex> findOwnedVertices(ObjectMapper om, Graph G, ID owner)
    {
        return G.getVertices(SYMBOLIC_OWNER, JSONUtilities.toJSON(om, getSymbolicID(owner)).toString());
    }

    /**
     * Find all edge owner by the specified ID, don't look at versions.
     */
    public static Iterable<Edge> findOwnedEdges(ObjectMapper om, Graph G, ID owner)
    {
        return G.getEdges(SYMBOLIC_OWNER, JSONUtilities.toJSON(om, getSymbolicID(owner)).toString());
    }

    /**
     * Find vertex without considering the version specified in the ID.
     */
    public static Vertex resolveVertex(ObjectMapper om, Graph G, ID id)
    {
        return findVertexOnProperty(om, G, getSymbolicID(id), SYMBOLIC_IDENTIFER);
    }

    public static EdgeID getEdgeID(ObjectMapper om, Edge edge)
    {
        final Vertex tail = edge.getVertex(Direction.OUT);
        final Vertex head = edge.getVertex(Direction.IN);

        final ID tailID = getID(om, tail);
        final ID headID = getID(om, head);

        if (tailID != null && headID != null)
            return new EdgeID(tailID, edge.getLabel(), headID);

        return null;
    }

    /**
     * TODO: return null if ID cannot be found
     */
    public static ID getID(ObjectMapper om, Vertex vertex)
    {
        final String json = String.valueOf(vertex.getProperty(IDENTIFIER));

        try
        {
            final JsonNode node = om.readTree(json);
            return JSONUtilities.fromJSON(node);
        }
        catch (IOException e)
        {
            log.trace("Failed to parse ID from string '{}' (should be JSON Array)", json);
            return null;
        }
    }

    public static void setID(ObjectMapper om, Vertex vertex, ID id)
    {
        final String json = JSONUtilities.toJSON(om, id).toString();
        vertex.setProperty(IDENTIFIER, json);

        final String symbolic = JSONUtilities.toJSON(om, getSymbolicID(id)).toString();
        vertex.setProperty(SYMBOLIC_IDENTIFER, symbolic);
    }

    public static void setEdgeId(ObjectMapper om, EdgeID edgeID, Edge edge) {
        edge.setProperty(IDENTIFIER, getStringRepresentation(om, edgeID));
    }

    public static ID getSymbolicID(ID id)
    {
        return new ID(id.index(), id.type(), id.id(), 0);
    }

    public static void setOwner(ObjectMapper om, Element element, ID id)
    {
        element.setProperty(OWNER, JSONUtilities.toJSON(om, id).toString());
        element.setProperty(SYMBOLIC_OWNER, JSONUtilities.toJSON(om, getSymbolicID(id)).toString());
    }

    /**
     * Find the owner of an edge or a vertex.
     */
    public static ID getOwner(ObjectMapper om, Element element)
    {
        final Object owner = element.getProperty(OWNER);
        if (owner == null)
            return null;

        try
        {
            final JsonNode node = om.readTree(String.valueOf(owner));
            return JSONUtilities.fromJSON(node);
        }
        catch (IOException e)
        {
            log.trace("Failed to parse ID from '{}' (should be JSON Array)", String.valueOf(owner));
            return null;
        }
    }

    /**
     * Create an edge in the graph and set its identifier.
     */
    public static Edge createEdge(ObjectMapper om, Graph G, EdgeID edgeID)
    {
        final Vertex tv = findVertex(om, G, edgeID.tail());
        final Vertex hv = findVertex(om, G, edgeID.head());

        if(tv == null || hv == null)
            throw new RuntimeException("Head or tail of edge doesn't exist!");

        final Edge e = G.addEdge(null, tv, hv, edgeID.label());

        setEdgeId(om, edgeID, e);
        return e;
    }

    /**
     * Create a vertex and set it's identifier.
     */
    public static Vertex createVertex(ObjectMapper om, Graph G, ID id)
    {
        final Vertex v = G.addVertex(null);
        setID(om, v, id);
        return v;
    }

    public static boolean isSymbolic(ObjectMapper om, final Vertex v)
    {
        return getID(om, v).isSymbolic();
    }

    /**
     * Return true if {@link GraphQueries#setOwner} can be called on this vertex without violating semantics:
     *
     * You should only set the owner if:
     * <ul>
     *     <li>The vertex does not have an owner yet</li>
     *     <li>An older version of your subset owns the vertex</li>
     *     <li>The vertex is a symbolic vertex ({@code version() == 0})</li>
     * </ul>
     */
    public static boolean isOwnable(ObjectMapper om, final Vertex v, final ID owner)
    {
        final ID id = getOwner(om, v);
        return id == null || onlyVersionDiffers(id, owner) || isSymbolic(om, v);
    }

    /**
     * Tests if a.version is smaller than b.version
     */
    public static boolean isOlder(final ID a, final ID b)
    {
        return a.version() < b.version();
    }

    /**
     * Determine whether two ID instances are equal when ignoring their versions.
     */
    public static boolean onlyVersionDiffers(final ID id, final ID other)
    {
        return id.id().equals(other.id())
                && id.index().equals(other.index())
                && id.type().equals(other.type());
    }

    /**
     * From the edge ID, return the ID that is not {@code notThisOne}.
     */
    public static ID getOppositeId(final EdgeID id, final ID notThisOne)
    {
        return id.head().equals(notThisOne) ? id.tail() : id.head();
    }

    /**
     * Return the direction <i>d</i> such that {@link Edge#getVertex(com.tinkerpop.blueprints.Direction d)}
     * return the vertex {@code !=} to the vertex with {@code getID() == vertexId}.
     */
    public static Direction directionOppositeTo(final EdgeID id, final ID vertexId)
    {
        return id.head().equals(vertexId) ? Direction.OUT : Direction.IN;
    }

    public static EdgeID createOppositeId(final EdgeID id, final ID notThisOne, final ID targetId)
    {
        if (id.tail().equals(notThisOne))
            return new EdgeID(id.tail(), id.label(), targetId);
        else
            return new EdgeID(targetId, id.label(), id.head());
    }

    public static void dumpGraph(ObjectMapper om, Graph graph)
    {
        log.trace("Graph dump start");

        for(Edge e : graph.getEdges())
        {
            final EdgeID edgeId = getEdgeID(om, e);
            if(edgeId == null)
                log.trace(e.toString());
            else
                log.trace(edgeId.toString());
        }

        log.trace("Graph dump done");
    }

    public static void makeSymbolic(ObjectMapper om, Vertex vertex)
    {
        final ID symbolicID = getSymbolicID(getID(om, vertex));
        setID(om, vertex, symbolicID);
        setOwner(om, vertex, symbolicID);

        for (Edge edge : vertex.getEdges(Direction.IN)) {
            EdgeID id = getEdgeID(om, edge);
            EdgeID idWithSymbolicHead = new EdgeID(id.tail(), id.label(), symbolicID);
            setEdgeId(om, idWithSymbolicHead, edge);
        }

        for (Edge edge : vertex.getEdges(Direction.OUT)) {
            EdgeID id = getEdgeID(om, edge);
            EdgeID idWithSymbolicHead = new EdgeID(symbolicID, id.label(), id.head());
            setEdgeId(om, idWithSymbolicHead, edge);
        }
    }
}