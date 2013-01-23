package graphs.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.tinkerpop.blueprints.*;
import degraphmalizr.EdgeID;
import degraphmalizr.ID;
import exceptions.DegraphmalizerException;
import graphs.GraphQueries;
import org.testng.annotations.*;

import java.util.Collections;
import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;


public class NewSubgraphManagerTest
{
    final Random random = new Random(System.currentTimeMillis());
    LocalGraph lg;
    GraphBuilder gb;
    @BeforeMethod
    public void clearGraph()
    {
        gb = new GraphBuilder(random.nextInt());
        lg = LocalGraph.localNode();
    }

    @AfterMethod
    public void shutdownGraph()
    {
        lg.G.shutdown();
    }

    @Test
    public void testGraphBuilder()
    {
        final GraphBuilder gb = new GraphBuilder(0);
        final EdgeID e1 = gb.edge("   (a,b,c,0) -- label --> (a,b,c,0)   ");

        assertThat(e1.head()).isEqualTo(e1.tail());
        assertThat(e1.head().version()).isEqualTo(0);
        assertThat(e1.tail().version()).isEqualTo(0);

        final GraphBuilder gb2 = new GraphBuilder(1);
        final EdgeID e2 = gb2.edge("   (a,b,c,0) -- label --> (a,b,c,0)   ");

        assertThat(e1.head()).isNotEqualTo(e2.head());
        assertThat(e2.head().version()).isEqualTo(0);

        final EdgeID e3 = gb.edge("   (a,b,c,1) -- label --> (d,e,f,2)   ");

        assertThat(e3.head()).isNotEqualTo(e3.tail());
        assertThat(e3.head().index()).isNotEqualTo(e3.tail().index());
        assertThat(e3.head().type()).isNotEqualTo(e3.tail().type());
        assertThat(e3.head().id()).isNotEqualTo(e3.tail().id());
        assertThat(e3.head().version()).isNotEqualTo(e3.tail().version());
        assertThat(e3.head().version()).isPositive();
        assertThat(e3.tail().version()).isPositive();
    }

    @Test
    public void testEdgeCreationToSymbolicVertex() throws DegraphmalizerException
    {
        // create some edge
        final EdgeID edge = gb.edge("(a,b,c,1) -- label --> (d,e,f,0)");
        final ID id = edge.tail();

        // create subgraph containing the "a,b,c,1" vertex
        final Subgraph subgraph1 = lg.sgm.createSubgraph(id);

        // add the edge
        addEdgeToSubgraph(subgraph1, id, edge);

        // modify the graph
        lg.sgm.commitSubgraph(subgraph1);

        // see if we have exactly enough elements in the graph
        assertThat(lg.G.getEdges()).hasSize(1);
        assertThat(lg.G.getVertices()).hasSize(2);

        // make sure it is our edge
        assertThatGraphContains(lg.G, edge);

        GraphQueries.dumpGraph(lg.G);

        // now lets claim "d,e,f,0"
        final EdgeID newEdge = gb.edge("(a,b,c,1) -- label --> (d,e,f,2)");
        final ID claim = newEdge.head();

        final Subgraph subgraph2 = lg.sgm.createSubgraph(claim);
        lg.sgm.commitSubgraph(subgraph2);

        GraphQueries.dumpGraph(lg.G);

        // see if we have exactly enough elements in the graph
        assertThat(lg.G.getEdges()).hasSize(1);
        assertThat(lg.G.getVertices()).hasSize(2);

        // make sure our edge is updated as expected
        assertThatGraphContains(lg.G, newEdge);
    }

    @Test
    public void testEdgeCreationToAlreadyExistingVertex() throws DegraphmalizerException
    {
        // create some edge
        final EdgeID edge = gb.edge("(a,b,c,1) -- label --> (d,e,f,2)");
        final ID source = edge.tail();
        final ID target = edge.head();

        // create and commit a subgraph containing the "d,e,f,2" vertex
        lg.sgm.commitSubgraph(lg.sgm.createSubgraph(target));

        assertThat(lg.G.getEdges()).isEmpty();
        assertThat(lg.G.getVertices()).hasSize(1);

        // create subgraph containing the "a,b,c,1" vertex
        final Subgraph source_sg = lg.sgm.createSubgraph(source);

        // add the edge
        addEdgeToSubgraph(source_sg, source, edge);

        // modify the graph
        lg.sgm.commitSubgraph(source_sg);

        // see if we have exactly enough elements in the graph
        assertThat(lg.G.getEdges()).hasSize(1);
        assertThat(lg.G.getVertices()).hasSize(2);

        // make sure it is our edge
        assertThatGraphContains(lg.G, edge);
    }


    // add the edge to the subgraph, the other side of the edge will be made symbolic
    protected void addEdgeToSubgraph(Subgraph sg, ID sg_id, EdgeID edge_id)
    {
        // TODO add subgraph.id() method, and change interface style (pull: getEdges() instead of push addEdge())
        final Direction d = GraphQueries.directionOppositeTo(edge_id, sg_id);
        final ID other = GraphQueries.getSymbolicID(GraphQueries.getOppositeId(edge_id, sg_id));
        sg.addEdge(edge_id.label(), other, d.opposite(), Collections.<String, JsonNode>emptyMap());
    }

    protected void assertThatGraphContains(Graph G, EdgeID edge_id)
    {
        final Vertex head = GraphQueries.findVertex(G, edge_id.head());
        final Vertex tail = GraphQueries.findVertex(G, edge_id.tail());
        final Edge edge = GraphQueries.findEdge(G, edge_id);

        assertThat(head).isNotNull();
        assertThat(tail).isNotNull();
        assertThat(edge).isNotNull();

        assertThat(edge.getVertex(Direction.IN)).isEqualTo(head);
        assertThat(edge.getVertex(Direction.OUT)).isEqualTo(tail);
    }
}