package degraphmalizr;

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

    public VID(Vertex v)
    {
        this.id = GraphQueries.getID(v);
        this.v = v;
    }

    public VID(Vertex v, ID id)
    {
        this.id = id;
        this.v = v;
    }

    public VID(Graph G, ID id)
    {
        this.id = id;
        this.v = GraphQueries.findVertex(G, id);
    }

    public boolean isCorrect()
    {
        return id.equals(GraphQueries.getID(v));
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
