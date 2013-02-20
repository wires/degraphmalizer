package graphs.ops;

import com.fasterxml.jackson.databind.JsonNode;
import degraphmalizr.ID;

import java.util.Map;

/**
TODO fix this documentation a bit
how is this interface complete:

- instead of removing from the graph, there is just no element generated for the next version of a document.

- every ID used, which is not explicitly "added" first will be considered 'symbolic' and created without data,
   once the graph is viewed, referring to some indirect reference to a node.
*/
public interface Subgraph
{
    enum Direction { INWARDS, OUTWARDS };

    interface Edge {
        ID other();
        Direction direction();
        String label();
        Map<String,JsonNode> properties();
    }

    Iterable<Edge> edges();

    Map<String,JsonNode> properties();
}