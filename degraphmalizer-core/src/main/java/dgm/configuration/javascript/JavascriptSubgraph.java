package dgm.configuration.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dgm.degraphmalizr.ID;
import dgm.graphs.ops.MutableSubgraph;
import dgm.graphs.ops.Subgraph;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Javascript interface to subgraph
 */
public class JavascriptSubgraph
{
    final MutableSubgraph subgraph = new MutableSubgraph();

    final private ObjectMapper om;
    final private Scriptable scope;
    final private Context cx;


    public JavascriptSubgraph(ObjectMapper om, Context cx, Scriptable scope)
    {
        this.om = om;
        this.cx = cx;
        this.scope = scope;
    }

    public final void addEdge(String label, String index, String type, String id, boolean inwards, Map<String, Object> properties) throws IOException
    {
        final ID other = new ID(index, type, id, 0);
        final Map<String,JsonNode> props = new HashMap<String, JsonNode>();

        // call subgraph method
        final Subgraph.Direction d;

        if(inwards)
            d = Subgraph.Direction.INWARDS;
        else
            d = Subgraph.Direction.OUTWARDS;

        final MutableSubgraph.Edge e = subgraph.beginEdge(label, other, d);

        for(Map.Entry<String,Object> p : properties.entrySet())
        {
            // convert into JsonNode
            final JsonNode result = JSONUtilities.fromJSONObject(om, cx, scope, p.getValue());

            // Store it
            e.property(p.getKey(), result);
        }
    }

    public final void setProperty(String key, Object value) throws IOException
    {
        // convert into JsonNode
        final JsonNode result = JSONUtilities.fromJSONObject(om, cx, scope, value);
        subgraph.property(key, result);
    }
}
