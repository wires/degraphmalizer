package graphs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tinkerpop.blueprints.*;

import degraphmalizr.EdgeID;
import degraphmalizr.ID;
import trees.Pair;
import trees.Tree;

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

    public static JsonNode getProperty(Element elt, String property)
    {
        checkPropertyName(property);

        final Object obj = elt.getProperty(property);

        if(! (obj instanceof String))
            throw new RuntimeException("Property " + property + " is not in the expected format (String), it's a "
                    + obj.getClass().getSimpleName());

        final ObjectMapper om = new ObjectMapper();
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

    public static Edge findEdge(Graph G, EdgeID edgeID)
    {
        final String id = getStringRepresentation(edgeID);
        final Iterator<Edge> ei = G.getEdges(IDENTIFIER, id).iterator();
        if(!ei.hasNext())
            return null;

        final Edge e = ei.next();

        if(ei.hasNext())
            throw new RuntimeException("Graph inconsistency! More than one edge with (head,label,tail) coordinate "
                    + id); // TODO: Consistently handle these inconsistencies

        return e;
    }

    private static String getStringRepresentation(final EdgeID edgeID)
    {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(toJsonStringRepresentation(edgeID.tail()))
                     .append("--")
                     .append(edgeID.label())
                     .append("->")
                     .append(toJsonStringRepresentation(edgeID.head()));
        return stringBuilder.toString();
    }


    private static Vertex findVertexOnProperty(Graph G, ID id, String propertyName)
    {
        final Iterator<Vertex> vi = G.getVertices(propertyName, toJsonStringRepresentation(id)).iterator();
        if(!vi.hasNext())
            return null;

        final Vertex v = vi.next();

        if(vi.hasNext())
            throw new RuntimeException("Graph inconsistency! More than one vertex with identifier " + id);

        return v;
    }

    public static Vertex findVertex(Graph G, ID id)
    {
        return findVertexOnProperty(G, id, IDENTIFIER);
    }

    /**
     * Find all vertices owner by the specified ID, don't look at versions.
     * This method will not return the ownable symbolic vertices.
     */
    public static Iterable<Vertex> findOwnedVertices(Graph G, ID owner)
    {
        return G.getVertices(SYMBOLIC_OWNER, toJsonStringRepresentation(getSymbolicID(owner)));
    }

    /**
     * Find all edge owner by the specified ID, don't look at versions.
     */
    public static Iterable<Edge> findOwnedEdges(Graph G, ID owner)
    {
        return G.getEdges(SYMBOLIC_OWNER, toJsonStringRepresentation(getSymbolicID(owner)));
    }

    /**
     * Find vertex without considering the version specified in the ID.
     */
    public static Vertex resolveVertex(Graph G, ID id)
    {
        return findVertexOnProperty(G, getSymbolicID(id), SYMBOLIC_IDENTIFER);
    }

    public static String toJsonStringRepresentation(ID id)
    {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ArrayNode n = objectMapper.createArrayNode();
        n.add(id.index()).add(id.type()).add(id.id()).add(id.version());
        return n.toString();
    }



    private static ID fromJsonStringRepresentation(String json)
    {
        try
        {
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode n = objectMapper.readTree(json);

            if(!n.isArray())
                return null;

            final ArrayNode a = (ArrayNode)n;
            final String index = a.get(0).textValue();
            final String type = a.get(1).textValue();
            final String id = a.get(2).textValue();
            final long version = a.get(3).longValue();

            return new ID(index,type,id,version);

        }
        catch (IOException e)
        {
            // log.trace("Failed to deserialize '" + json + "' into an ID");
            return null;
        }
    }

    public static EdgeID getEdgeID(Edge edge)
    {
        final Vertex tail = edge.getVertex(Direction.OUT);
        final Vertex head = edge.getVertex(Direction.IN);

        final ID tailID = getID(tail);
        final ID headID = getID(head);

        if (tailID != null && headID != null)
            return new EdgeID(tailID, edge.getLabel(), headID);

        return null;
    }

    /**
     * TODO: return null if ID cannot be found
     */
    public static ID getID(Vertex vertex)
    {
        final String json = String.valueOf(vertex.getProperty(IDENTIFIER));
        return fromJsonStringRepresentation(json);
    }

    public static void setID(Vertex vertex, ID id)
    {
        final String json = toJsonStringRepresentation(id);
        vertex.setProperty(IDENTIFIER, json);

        final String symbolic = toJsonStringRepresentation(getSymbolicID(id));
        vertex.setProperty(SYMBOLIC_IDENTIFER, symbolic);
    }

    public static void setEdgeId(EdgeID edgeID, Edge edge) {
        edge.setProperty(IDENTIFIER, getStringRepresentation(edgeID));
    }

    public static ID getSymbolicID(ID id)
    {
        return new ID(id.index(), id.type(), id.id(), 0);
    }

    public static void setOwner(Element element, ID id)
    {
        element.setProperty(OWNER, toJsonStringRepresentation(id));
        element.setProperty(SYMBOLIC_OWNER, toJsonStringRepresentation(getSymbolicID(id)));
    }

    /**
     * Find the owner of an edge or a vertex.
     */
    public static ID getOwner(Element element)
    {
        final Object owner = element.getProperty(OWNER);
        if (owner == null)
            return null;

        return fromJsonStringRepresentation(String.valueOf(owner));
    }

    /**
     * Create an edge in the graph and set its identifier.
     */
    public static Edge createEdge(Graph G, EdgeID edgeID)
    {
        final Vertex tv = findVertex(G, edgeID.tail());
        final Vertex hv = findVertex(G, edgeID.head());

        if(tv == null || hv == null)
            throw new RuntimeException("Head or tail of edge doesn't exist!");

        final Edge e = G.addEdge(null, tv, hv, edgeID.label());

        setEdgeId(edgeID, e);
        return e;
    }

    /**
     * Create a vertex and set it's identifier.
     */
    public static Vertex createVertex(Graph G, ID id)
    {
        final Vertex v = G.addVertex(null);
        setID(v, id);
        return v;
    }

    public static boolean isSymbolic(final Vertex v)
    {
        return getID(v).isSymbolic();
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
    public static boolean isOwnable(final Vertex v, final ID owner)
    {
        final ID id = getOwner(v);
        return id == null || onlyVersionDiffers(id, owner) || isSymbolic(v);
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

    public static void dumpGraph(Graph graph)
    {
        log.trace("Graph dump start");

        for(Edge e : graph.getEdges())
        {
            final EdgeID edgeId = getEdgeID(e);
            if(edgeId == null)
                log.trace(e.toString());
            else
                log.trace(edgeId.toString());
        }

        log.trace("Graph dump done");
    }

    public static void makeSymbolic(Vertex vertex)
    {
        final ID symbolicID = getSymbolicID(getID(vertex));
        setID(vertex, symbolicID);
        setOwner(vertex, symbolicID);

        for (Edge edge : vertex.getEdges(Direction.IN)) {
            EdgeID id = getEdgeID(edge);
            EdgeID idWithSymbolicHead = new EdgeID(id.tail(), id.label(), symbolicID);
            setEdgeId(idWithSymbolicHead, edge);
        }

        for (Edge edge : vertex.getEdges(Direction.OUT)) {
            EdgeID id = getEdgeID(edge);
            EdgeID idWithSymbolicHead = new EdgeID(symbolicID, id.label(), id.head());
            setEdgeId(idWithSymbolicHead, edge);
        }
    }
}