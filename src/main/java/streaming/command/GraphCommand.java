package streaming.command;

import java.util.Collections;
import java.util.List;

public class GraphCommand {

    private final GraphCommandType typeGraph;
    private final List<GraphNode> nodes;

    GraphCommand(GraphCommandType typeGraph, List<GraphNode> nodes) {
        this.typeGraph = typeGraph;
        this.nodes = nodes;
    }

    public GraphCommandType getCommandType() {
        return typeGraph;
    }

    public List<GraphNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
}