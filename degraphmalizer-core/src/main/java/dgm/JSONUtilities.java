package dgm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
}