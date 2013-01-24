package configuration.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import degraphmalizr.EdgeID;
import degraphmalizr.ID;
import graphs.GraphQueries;

import org.mozilla.javascript.*;

import java.io.IOException;

/**
 * Some helper functions to convert between Object (coming from Rhino) and {@link JsonNode}
 */
public final class JSONUtilities
{
    private JSONUtilities() {}

    /**
     * Transform a JSON string into a JS Object
     */
    public static Object toJSONObject(Context cx, Scriptable scope, String json)
    {
        // TODO compile and cache the reviver function once

        // we can use the reviver to convert date strings to date objects (if needed)
        final String reviverjs = "(function(key,value) { return value; })";
        final Callable reviver = (Callable) cx.evaluateString(scope, reviverjs, "reviver", 0, null);

        return NativeJSON.parse(cx, scope, json, reviver);
    }

    /**
     * Parse JS Object back into a JsonNode
     */
    public static JsonNode fromJSONObject(ObjectMapper om, Context cx, Scriptable scope, Object obj) throws IOException
    {
        // convert javascript object to JSON string
        final String objectJson = (String) NativeJSON.stringify(cx, scope, obj, null, null);

        // convert JSON string to JsonNode
        return om.readTree(objectJson);
    }

    public static ObjectNode toJSON(final ObjectMapper om, final Edge edge)
    {
        final ObjectNode objectNode = om.createObjectNode();

        final EdgeID edgeID = GraphQueries.getEdgeID(edge);

        final ID tail = edgeID.tail();
        objectNode.put("tail", toJSON(om, tail));

        final String label = edgeID.label();
        objectNode.put("label", label);

        final ID head = edgeID.head();
        objectNode.put("head", toJSON(om, head));

        return objectNode;
    }

    public static ObjectNode toJSON(ObjectMapper om, Vertex vertex)
    {
        final ID id = GraphQueries.getID(vertex);
        return toJSON(om, id);
    }

    public static ObjectNode toJSON(ObjectMapper om, ID id)
    {
        final ObjectNode n = om.createObjectNode();
        n.put("index", id.index());
        n.put("type", id.type());
        n.put("id", id.id());
        n.put("version", id.version());
        return n;
    }
}