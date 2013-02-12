package configuration.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import degraphmalizr.EdgeID;
import degraphmalizr.ID;
import degraphmalizr.recompute.RecomputeResult;
import graphs.GraphQueries;
import org.elasticsearch.action.index.IndexResponse;
import org.mozilla.javascript.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

    public static ArrayNode toJSON(ObjectMapper om, Vertex vertex)
    {
        final ID id = GraphQueries.getID(vertex);
        return toJSON(om, id);
    }

    public static ArrayNode toJSON(ObjectMapper om, ID id)
    {
        final ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.createArrayNode()
                .add(id.index())
                .add(id.type())
                .add(id.id())
                .add(id.version());
    }

    public static ObjectNode renderException(ObjectMapper om, Throwable t)
    {
        final ArrayNode ss = om.createArrayNode();
        for(StackTraceElement elt : t.getStackTrace())
            ss.add(elt.toString());

        final ObjectNode ex = om.createObjectNode();
        ex.put("message", t.getMessage());
        ex.put("class", t.getClass().getSimpleName());
        ex.put("stacktrace", ss);

        return ex;
    }

    public static ID fromJSON(JsonNode n)
    {
        if(!n.isArray())
            return null;

        final ArrayNode a = (ArrayNode)n;
        final String index = a.get(0).textValue();
        final String type = a.get(1).textValue();
        final String id = a.get(2).textValue();
        final long version = a.get(3).longValue();

        return new ID(index,type,id,version);
    }

    public static ObjectNode toJSON(ObjectMapper objectMapper, RecomputeResult ourResult) throws InterruptedException, ExecutionException
    {
        if(ourResult.success().isPresent())
        {
            final ObjectNode n = objectMapper.createObjectNode();

            final RecomputeResult.Success success = ourResult.success().get();
            n.put("success", true);

            // write targetID using index reponse
            final IndexResponse ir = success.indexResponse();
            final ObjectNode targetID = objectMapper.createObjectNode();
            targetID.put("index", ir.index());
            targetID.put("type", ir.type());
            targetID.put("id", ir.id());
            targetID.put("version", ir.version());
            n.put("targetID", targetID);

            // write dictionary of properties and their values
            final ObjectNode properties = objectMapper.createObjectNode();
            for(Map.Entry<String,JsonNode> entry : success.properties().entrySet())
                properties.put(entry.getKey(), entry.getValue());
            n.put("properties", properties);

            // write dictionary of properties and their values
            n.put("sourceDocumentAfterTransform", success.sourceDocument());

            return n;
        }

        if(ourResult.exception().isPresent())
        {
            final Throwable t = ourResult.exception().get();
            final ObjectNode n = JSONUtilities.renderException(objectMapper, t);
            n.put("success", false);
            return n;
        }

        if(ourResult.expired().isPresent())
        {
            final ObjectNode n = objectMapper.createObjectNode();
            final ArrayNode ids = objectMapper.createArrayNode();
            n.put("success", false);
            for(ID id : ourResult.expired().get())
                ids.add(JSONUtilities.toJSON(objectMapper, id));
            return n;
        }

        final ObjectNode n = objectMapper.createObjectNode();
        n.put("success", false);
        n.put("status", ourResult.status().name());
        return n;
    }

}