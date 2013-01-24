package graphs.ops;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.tinkerpop.blueprints.Direction;

import degraphmalizr.ID;

/**
TODO fix this documentation a bit
how is this interface complete:

- instead of removing from the graph, there is just no element generated for the next version of a document.

- every ID used, which is not explicitly "added" first will be considered 'symbolic' and created without data,
   once the graph is viewed, referring to some indirect reference to a node.
*/
public interface Subgraph
{
    /**
     * Add an edge from ({@code d == OUT}) or to ({@code d == IN}) the central node.
     *
     * The three parameters (label, other, direction) unique identify an edge. When
     * this method is called on an already existing edge, that edge's properties are
     * overwritten.
     *
     * @param label the edge label
     * @param other the edge always runs between the central node and this other node
     * @param direction
     * @param properties
     */
    void addEdge(String label, ID other, Direction direction, Map<String, JsonNode> properties);

    /**
     * Set property on the central node.
     *
     * When called on the same key twice, the value for that key is overwritten.
     *
     * @param key
     * @param value
     */
    void setProperty(String key, JsonNode value);
}