package degraphmalizr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import graphs.GraphQueries;

/**
 * Vertex and it's ID.
 */
public class VID
{
    final ID id;
    final Vertex v;

    final ObjectMapper om;

    public VID(ObjectMapper om, Vertex v)
    {
        this.om = om;
        this.id = GraphQueries.getID(om, v);
        this.v = v;
    }

    public VID(ObjectMapper om, Vertex v, ID id)
    {
        this.om = om;
        this.id = id;
        this.v = v;
    }

    public VID(ObjectMapper om, Graph G, ID id)
    {
        this.om = om;
        this.id = id;
        this.v = GraphQueries.findVertex(om, G, id);
    }

    public boolean isCorrect()
    {
        return id.equals(GraphQueries.getID(om, v));
    }

    public final ID ID()
    {
        return id;
    }

    public final Vertex vertex()
    {
        return v;
    }
}
