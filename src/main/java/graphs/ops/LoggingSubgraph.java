package graphs.ops;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import degraphmalizr.ID;

public class LoggingSubgraph implements Subgraph
{
	Logger log = LoggerFactory.getLogger("subgraph");
	
	final String prefix;
	
	public LoggingSubgraph(String prefix)
	{
		this.prefix = prefix;
	}

    @Override
    public void addEdge(String label, ID other, Direction direction, Map<String, JsonNode> properties)
    {
        final String d = direction == Direction.IN ? "to" : "from";
        log.info(prefix + " edge " + d + " self to " + other + " with label " + label + " created");
    }

    @Override
    public void setProperty(String key, JsonNode value)
    {
        log.info(prefix + " property " + key + " set to " + value.toString());
    }
}
