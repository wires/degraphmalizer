package streaming.service;

import com.tinkerpop.blueprints.*;
import org.slf4j.Logger;
import streaming.blueprints.GraphCommandListener;

import javax.inject.Inject;

import static streaming.command.GraphCommandBuilder.*;
import static streaming.command.GraphCommandBuilder.edge;

/**
 *
 */
public class GraphUnfoldingServiceImpl implements GraphUnfoldingService
{
    @Inject
    Logger log;

    final Graph graph;

    @Inject
    public GraphUnfoldingServiceImpl(Graph graph)
    {
        this.graph = graph;
    }

    @Override
    public final void unfoldVertex(String id, GraphCommandListener listener)
    {
        final Vertex v = graph.getVertex(id);

        // not found
        if(v == null)
        {
            log.debug("No such node {}", id);
            return;
        }

        // create node
        listener.commandCreated(addNodeCommand(node((String) v.getId())).build());

        // walk 1 level deep from id
        for(Direction d  : new Direction[]{Direction.IN, Direction.OUT})
        {
            for(Edge e : v.getEdges(d))
            {
                final Vertex v2 = e.getVertex(d.opposite());
                final String v2Id = (String)v2.getId();
                final String eId = (String)e.getId();
                final String eLabel = e.getLabel();

                listener.commandCreated(addNodeCommand(node(v2Id)).build());
                listener.commandCreated(addEdgeCommand(edge(eId, id, v2Id, true).set("label", eLabel)).build());
            }
        }
    }
}