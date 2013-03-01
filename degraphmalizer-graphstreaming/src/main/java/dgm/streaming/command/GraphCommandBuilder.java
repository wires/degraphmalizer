package dgm.streaming.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GraphCommandBuilder {

    private GraphCommandBuilder() {}

    public static NodeCommandBuilder addNodeCommand(NodeBuilder nodeBuilder) {
        return new NodeCommandBuilder(GraphCommandType.AddNode, nodeBuilder);
    }

    public static NodeCommandBuilder deleteNodeCommand(String... nodeNames) {
        return createDeleteCommandBuilder(GraphCommandType.DeleteNode, nodeNames);
    }

    public static NodeCommandBuilder updateNodeCommand(NodeBuilder nodeBuilder) {
        return new NodeCommandBuilder(GraphCommandType.ChangeNode, nodeBuilder);
    }

    public static NodeCommandBuilder addEdgeCommand(NodeBuilder nodeBuilder) {
        return new NodeCommandBuilder(GraphCommandType.AddEdge, nodeBuilder);
    }

    public static NodeCommandBuilder deleteEdgeCommand(String... nodeNames) {
        return createDeleteCommandBuilder(GraphCommandType.DeleteEdge, nodeNames);
    }

    private static NodeCommandBuilder createDeleteCommandBuilder(GraphCommandType graphCommandType, String... nodeNames) {
        if (nodeNames.length == 0) {
            throw new IllegalArgumentException("Give a least one node name");
        }
        NodeCommandBuilder ncb = new NodeCommandBuilder(graphCommandType, node(nodeNames[0]));
        if (nodeNames.length > 1) {
            for (int i = 1; i < nodeNames.length; i++) {
                ncb.addNode(node(nodeNames[i]));
            }
        }
        return ncb;
    }

    public static NodeCommandBuilder updateEdgeCommand(NodeBuilder nodeBuilder) {
        return new NodeCommandBuilder(GraphCommandType.ChangeEdge, nodeBuilder);
    }

    public static NodeBuilder node(String name, int size) {
        return new NodeBuilder(name).set("size", size);
    }

    public static NodeBuilder node(String name) {
        return new NodeBuilder(name);
    }

    public static NodeBuilder edge(String name, String source, String destination, boolean directed, int size) {
        return new NodeBuilder(name)
                .set("source", source)
                .set("target", destination)
                .set("directed", directed)
                .set("size", size);
    }

    public static NodeBuilder edge(String name, String source, String destination, boolean directed) {
        return edge(name, source, destination, directed, 1);
    }

    public static final class NodeCommandBuilder {
        private final GraphCommandType graphCommandType;
        private List<NodeBuilder> nodeBuilders = new ArrayList<NodeBuilder>();

        private NodeCommandBuilder(GraphCommandType graphCommandType, NodeBuilder nodeBuilder) {
            nodeBuilders.add(nodeBuilder);
            this.graphCommandType = graphCommandType;
        }

        public NodeCommandBuilder addNode(NodeBuilder nodeBuilder) {
            this.nodeBuilders.add(nodeBuilder);
            return this;
        }

        public GraphCommand build() {
            List<GraphNode> nodes = new ArrayList<GraphNode>();
            for (NodeBuilder nodeBuilder : nodeBuilders) {
                if ((graphCommandType == GraphCommandType.AddNode || graphCommandType == GraphCommandType.AddEdge) && nodeBuilder.properties.get("size") == null) {
                    //set the default size
                    nodeBuilder.properties.put("size", 1);
                }
                nodes.add(nodeBuilder.build());
            }
            return new GraphCommand(graphCommandType, nodes);
        }
    }

    public static class NodeBuilder {
        private final String nodeName;
        private Map<String, Object> properties = new HashMap<String, Object>();

        NodeBuilder(String nodeName) {
            this.nodeName = nodeName;
        }

        public final NodeBuilder set(String name, Object value) {
            properties.put(name, value);
            return this;
        }

        private GraphNode build() {
            return new GraphNode(nodeName, properties);
        }
    }
}