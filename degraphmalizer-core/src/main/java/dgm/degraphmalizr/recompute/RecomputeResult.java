package dgm.degraphmalizr.recompute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.index.IndexResponse;

import java.util.Map;

public class RecomputeResult
{
    protected final IndexResponse ir;
    protected final JsonNode source;
    protected final ObjectNode result;
    protected final Map<String, JsonNode> properties;

    public RecomputeResult(IndexResponse ir, JsonNode source, ObjectNode result, Map<String, JsonNode> properties)
    {
        this.ir = ir;
        this.source = source;
        this.result = result;
        this.properties = properties;
    }

    public IndexResponse indexResponse()
    {
        return ir;
    }

    public JsonNode sourceDocument()
    {
        return source;
    }

    public ObjectNode resultDocument()
    {
        return result;
    }

    public Map<String, JsonNode> properties()
    {
        return properties;
    }
}