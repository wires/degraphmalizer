package streaming.blueprints;

import com.tinkerpop.blueprints.*;
import streaming.command.GraphCommand;

import java.util.*;

import static streaming.command.GraphCommandBuilder.*;

public final class StreamingGraph implements Graph {

    private final List<GraphCommandListener> graphCommandListeners = new ArrayList<GraphCommandListener>();
    private final Graph wrapped;

    public StreamingGraph(Graph wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Features getFeatures() {
        return wrapped.getFeatures();
    }

    @Override
    public Vertex addVertex(Object o) {
        GraphCommand command = addNodeCommand(node(o.toString())).build();
        notifyGraphCommandListeners(command);
        return wrapped.addVertex(o);
    }

    @Override
    public Vertex getVertex(Object o) {
        return wrapped.getVertex(o);
    }

    @Override
    public void removeVertex(Vertex vertex) {
        GraphCommand command = deleteNodeCommand(vertex.getId().toString()).build();
        notifyGraphCommandListeners(command);
        wrapped.removeVertex(vertex);
    }

    @Override
    public Iterable<Vertex> getVertices() {

        return new Iterable<Vertex>() {
            @Override
            public Iterator<Vertex> iterator() {
                return new VertexIteratorWrapper(wrapped.getVertices().iterator());
            }
        };
    }

    @Override
    public Iterable<Vertex> getVertices(final String s, final Object o) {
        return new Iterable<Vertex>() {
            @Override
            public Iterator<Vertex> iterator() {
                return new VertexIteratorWrapper(wrapped.getVertices(s, o).iterator());
            }
        };
    }

    @Override
    public Edge addEdge(Object o, Vertex vertex, Vertex vertex1, String s) {
        NodeBuilder edgeBuilder = edge(o.toString(), vertex.getId().toString(), vertex1.getId().toString(), true)
                .set("label", s);
        notifyGraphCommandListeners(addEdgeCommand(edgeBuilder).build());
        return new StreamingEdge(wrapped.addEdge(o, vertex, vertex1, s));
    }

    @Override
    public Edge getEdge(Object o) {
        return new StreamingEdge(wrapped.getEdge(o));
    }

    @Override
    public void removeEdge(Edge edge) {
        notifyGraphCommandListeners(deleteEdgeCommand(edge.getId().toString()).build());
        wrapped.removeEdge(edge);
    }

    @Override
    public Iterable<Edge> getEdges() {
        return new Iterable<Edge>() {
            @Override
            public Iterator<Edge> iterator() {
                return new EdgeIteratorWrapper(wrapped.getEdges().iterator());
            }
        };
    }

    @Override
    public Iterable<Edge> getEdges(final String s, final Object o) {
        return new Iterable<Edge>() {
            @Override
            public Iterator<Edge> iterator() {
                return new EdgeIteratorWrapper(wrapped.getEdges(s, o).iterator());
            }
        };
    }

    @Override
    public void shutdown() {
        wrapped.shutdown();
    }

    public void addGraphCommandListener(GraphCommandListener graphCommandListener) {
        graphCommandListeners.add(graphCommandListener);
    }

    public void removeGraphCommandListener(GraphCommandListener graphCommandListener) {
        graphCommandListeners.remove(graphCommandListener);
    }

    public List<GraphCommandListener> findListeners(GraphCommandListenerFilter filter) {
        List<GraphCommandListener> found = new ArrayList<GraphCommandListener>();
        for (GraphCommandListener listener : graphCommandListeners) {
            if (filter.matches(listener)) found.add(listener);
        }
        return Collections.unmodifiableList(found);
    }

    protected void notifyGraphCommandListeners(GraphCommand graphCommand) {
        //simple way to get a consistent view on a possibly changing collection
        for (GraphCommandListener listener : Collections.unmodifiableList(graphCommandListeners)) {
            listener.commandCreated(graphCommand);
        }
    }

    public interface GraphCommandListenerFilter
    {
        boolean matches(GraphCommandListener listener);
    }

    final class StreamingVertex implements Vertex {

        private final Vertex wrapped;

        StreamingVertex(Vertex wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Iterable<Edge> getEdges(Direction direction, String... labels) {
            return wrapped.getEdges(direction, labels);
        }

        @Override
        public Iterable<Vertex> getVertices(final Direction direction, final String... labels) {
            return new Iterable<Vertex>() {
                @Override
                public Iterator<Vertex> iterator() {
                    return new VertexIteratorWrapper(wrapped.getVertices(direction, labels).iterator());
                }
            };
        }

        @Override
        public Query query() {
            return wrapped.query();
        }

        @Override
        public Object getProperty(String key) {
            return wrapped.getProperty(key);
        }

        @Override
        public Set<String> getPropertyKeys() {
            return wrapped.getPropertyKeys();
        }

        @Override
        public void setProperty(String key, Object value) {
            notifyGraphCommandListeners(updateNodeCommand(node(wrapped.getId().toString()).set(key, value)).build());
            wrapped.setProperty(key, value);
        }

        /**
         * Delete the property, and then create a node command to update the existing node with
         * the remaining properties
         *
         */
        @Override
        public Object removeProperty(String key) {
            notifyGraphCommandListeners(updateNodeCommand(node(wrapped.getId().toString()).set(key, null)).build());
            return wrapped.removeProperty(key);
        }

        @Override
        public Object getId() {
            return wrapped.getId();
        }
    }

    final class StreamingEdge implements Edge {

        private final Edge wrapped;

        StreamingEdge(Edge wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Vertex getVertex(Direction direction)
        {
            return new StreamingVertex(wrapped.getVertex(direction));
        }

        @Override
        public String getLabel() {
            return wrapped.getLabel();
        }

        @Override
        public Object getProperty(String key) {
            return wrapped.getProperty(key);
        }

        @Override
        public Set<String> getPropertyKeys() {
            return wrapped.getPropertyKeys();
        }

        @Override
        public void setProperty(String key, Object value) {
            NodeBuilder edgeBuilder = node(wrapped.getId().toString()).set(key, value);
            notifyGraphCommandListeners(updateEdgeCommand(edgeBuilder).build());
            wrapped.setProperty(key, value);
        }

        @Override
        public Object removeProperty(String key) {
            NodeBuilder edgeBuilder = node(wrapped.getId().toString()).set(key, null);
            notifyGraphCommandListeners(updateEdgeCommand(edgeBuilder).build());
            return wrapped.removeProperty(key);
        }

        @Override
        public Object getId() {
            return wrapped.getId();
        }
    }

    final class VertexIteratorWrapper implements Iterator<Vertex> {

        private final Iterator<Vertex> wrappedIterator;

        VertexIteratorWrapper(Iterator<Vertex> wrapped) {
            this.wrappedIterator = wrapped;
        }

        @Override
        public boolean hasNext() {
            return wrappedIterator.hasNext();
        }

        @Override
        public Vertex next() {
            return new StreamingVertex(wrappedIterator.next());
        }

        @Override
        public void remove() {
            wrappedIterator.remove();
        }
    }

    final class EdgeIteratorWrapper implements Iterator<Edge> {

        private final Iterator<Edge> edgeIterator;

        EdgeIteratorWrapper(Iterator<Edge> wrapped) {
            this.edgeIterator = wrapped;
        }

        @Override
        public boolean hasNext() {
            return edgeIterator.hasNext();
        }

        @Override
        public Edge next() {
            return new StreamingEdge(edgeIterator.next());
        }

        @Override
        public void remove() {
            edgeIterator.remove();
        }
    }
}