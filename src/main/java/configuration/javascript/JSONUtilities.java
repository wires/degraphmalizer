package configuration.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import degraphmalizr.ID;
import org.mozilla.javascript.*;

import java.io.IOException;

/**
 * Some helper functions to convert between Object (coming from Rhino) and {@link JsonNode}
 */
public class JSONUtilities
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