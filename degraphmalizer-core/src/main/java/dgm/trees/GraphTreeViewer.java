package dgm.trees;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import java.util.Iterator;

/** Traverse a graph as if it is a tree */
public class GraphTreeViewer implements TreeViewer<Pair<Edge, Vertex>>
{
    protected final Direction direction;

    public GraphTreeViewer(Direction direction)
    {
        this.direction = direction;
    }

    // iterator over all outgoing edges, returning a pair with the edge and it's other vertex
    class EIterator implements Iterator<Pair<Edge, Vertex>>
    {
        final Iterator<Edge> edge_iterator;

        public EIterator(Iterator<Edge> edge_iterator)
        {
            this.edge_iterator = edge_iterator;
        }

        @Override
        public boolean hasNext()
        {
            return edge_iterator.hasNext();
        }

        @Override
        public Pair<Edge, Vertex> next()
        {
            final Edge edge = edge_iterator.next();
            return new Pair<Edge,Vertex>(edge, edge.getVertex(direction.opposite()));
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("remove not implemented");
        }
    }

    @Override
    public Iterable<Pair<Edge, Vertex>> children(Pair<Edge, Vertex> node)
    {
        final Iterable<Edge> edges = node.b.getEdges(direction);
        return new Iterable<Pair<Edge, Vertex>>()
        {
            @Override
            public Iterator<Pair<Edge, Vertex>> iterator()
            {
                return new GraphTreeViewer.EIterator(edges.iterator());
            }
        };
    }
}
