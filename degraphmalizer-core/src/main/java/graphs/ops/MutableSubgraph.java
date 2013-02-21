package graphs.ops;

import com.fasterxml.jackson.databind.JsonNode;
import degraphmalizr.ID;

import java.util.*;

public class MutableSubgraph implements Subgraph
{
    final Map<String,JsonNode> properties = new HashMap<String,JsonNode>();
    final ArrayList<Edge> edges = new ArrayList<Edge>();

    @Override
    public Iterable<Subgraph.Edge> edges()
    {
        return (Iterable)edges;
    }

    @Override
    public Map<String, JsonNode> properties()
    {
        return properties;
    }

    public class Edge implements Subgraph.Edge
    {
        ID other;
        Direction direction;
        String label;
        Map<String,JsonNode> properties = new HashMap<String,JsonNode>();

        Edge(ID other, Direction d, String label)
        {
            this.other = other;
            this.direction = d;
            this.label = label;
        }

        @Override
        public ID other()
        {
            return other;
        }

        @Override
        public Direction direction()
        {
            return direction;
        }

        @Override
        public String label()
        {
            return label;
        }

        @Override
        public Map<String, JsonNode> properties()
        {
            return properties;
        }

        public MutableSubgraph.Edge other(ID other)
        {
            this.other = other;
            return this;
        }

        public MutableSubgraph.Edge direction(Direction direction)
        {
            this.direction = direction;
            return this;
        }

        public MutableSubgraph.Edge label(String label)
        {
            this.label = label;
            return this;
        }

        public MutableSubgraph.Edge property(String key, JsonNode value)
        {
            properties.put(key, value);
            return this;
        }

        public MutableSubgraph endEdge()
        {
            return MutableSubgraph.this;
        }

    }

    public MutableSubgraph.Edge beginEdge(String label, ID other, Direction d)
    {
        final Edge e = new Edge(other, d, label);
        edges.add(e);
        return e;
    }

    public MutableSubgraph property(String key, JsonNode value)
    {
        properties.put(key, value);
        return this;
    }
}
