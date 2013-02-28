package dgm.degraphmalizr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import dgm.GraphUtilities;
import dgm.ID;

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
        this.id = GraphUtilities.getID(om, v);
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
        this.v = GraphUtilities.findVertex(om, G, id);
    }

    public boolean isCorrect()
    {
        return id.equals(GraphUtilities.getID(om, v));
    }

    public final ID id()
    {
        return id;
    }

    public final Vertex vertex()
    {
        return v;
    }
}
