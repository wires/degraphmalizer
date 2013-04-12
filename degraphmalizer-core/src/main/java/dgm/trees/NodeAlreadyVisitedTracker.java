package dgm.trees;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: wires
 * Date: 3/20/13
 * Time: 12:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class NodeAlreadyVisitedTracker implements OccurrenceTracker<Pair<Edge,Vertex>>
{
    HashSet<Vertex> visited = new HashSet<Vertex>();

    @Override
    public boolean hasOccurred(Pair<Edge, Vertex> element)
    {
        final boolean in = visited.contains(element.b);
        if(!in)
            visited.add(element.b);
        return in;
    }
}
