package configuration.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tinkerpop.blueprints.Direction;
import degraphmalizr.ID;
import graphs.ops.Subgraph;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Javascript interface to subgraph
 */
//TODO not like this plz
public class JavascriptSubgraph
{
    final ObjectMapper om = new ObjectMapper();
    final Subgraph subgraph;
    final Scriptable scope;
    final Context cx;

    public JavascriptSubgraph(Subgraph subgraph, Context cx, Scriptable scope)
    {
        this.subgraph = subgraph;
        this.cx = cx;
        this.scope = scope;
    }

    public void addEdge(String label, String index, String type, String id, boolean inwards, Map<String, Object> properties) throws IOException
    {
        final ID other = new ID(index, type, id, 0);
        final Map<String,JsonNode> props = new HashMap<String, JsonNode>();

        for(Map.Entry<String,Object> p : properties.entrySet())
        {
            // convert into JsonNode
            final JsonNode result = JSONUtilities.fromJSONObject(om, cx, scope, p.getValue());

            // Store it
            props.put(p.getKey(), result);
        }

        // call subgraph method
        subgraph.addEdge(label, other, inwards ? Direction.IN : Direction.OUT, props);
    }

    public void setProperty(String key, Object value) throws IOException
    {
        // convert into JsonNode
        final JsonNode result = JSONUtilities.fromJSONObject(om, cx, scope, value);

        subgraph.setProperty(key, result);
    }
}
