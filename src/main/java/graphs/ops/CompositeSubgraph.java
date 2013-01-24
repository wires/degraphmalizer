package graphs.ops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.tinkerpop.blueprints.Direction;

import degraphmalizr.ID;

public class CompositeSubgraph implements Subgraph
{

	private final List<Subgraph> subgraphs;
	
	public CompositeSubgraph(Subgraph... subgraphs)
	{
		this.subgraphs = new ArrayList<Subgraph>(Arrays.asList(subgraphs));
	}

    @Override
    public final void addEdge(String label, ID other, Direction direction, Map<String, JsonNode> properties)
    {
        for(Subgraph s : subgraphs)
            s.addEdge(label, other, direction, properties);
    }

    @Override
    public final void setProperty(String key, JsonNode value)
    {
        for(Subgraph s : subgraphs)
            s.setProperty(key, value);
    }
}
