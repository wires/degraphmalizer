package dgm.graphs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.*;
import dgm.*;
import dgm.exceptions.DegraphmalizerException;
import org.fest.assertions.Assertions;
import org.testng.annotations.*;

import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;


public class NewSubgraphManagerTest
{
    final Random random = new Random(System.currentTimeMillis());
    LocalGraph lg;
    RandomizedGraphBuilder gb;

    @BeforeMethod
    public void clearGraph()
    {
        gb = new RandomizedGraphBuilder(random.nextInt());
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
        final RandomizedGraphBuilder gb = new RandomizedGraphBuilder(0);
        final EdgeID e1 = gb.edge("   (a,b,c,0) -- label --> (a,b,c,0)   ");

        assertThat(e1.head()).isEqualTo(e1.tail());
        assertThat(e1.head().version()).isEqualTo(0);
        assertThat(e1.tail().version()).isEqualTo(0);

        final RandomizedGraphBuilder gb2 = new RandomizedGraphBuilder(1);
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
        final MutableSubgraph subgraph1 = new MutableSubgraph();

        // add the edge
        addEdgeToSubgraph(subgraph1, id, edge);

        // modify the graph
        lg.sgm.commitSubgraph(id, subgraph1);

        // see if we have exactly enough elements in the graph
        assertThat(lg.G.getEdges()).hasSize(1);
        assertThat(lg.G.getVertices()).hasSize(2);

        // make sure it is our edge
        assertThatGraphContains(lg.G, edge);

        // now lets claim "d,e,f,0"
        final EdgeID newEdge = gb.edge("(a,b,c,1) -- label --> (d,e,f,2)");
        final ID claim = newEdge.head();

        lg.sgm.commitSubgraph(claim, new MutableSubgraph());

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
        lg.sgm.commitSubgraph(target, new MutableSubgraph());

        assertThat(lg.G.getEdges()).isEmpty();
        assertThat(lg.G.getVertices()).hasSize(1);

        // create subgraph containing the "a,b,c,1" vertex
        final MutableSubgraph source_sg = new MutableSubgraph();

        // add the edge
        addEdgeToSubgraph(source_sg, source, edge);

        // modify the graph
        lg.sgm.commitSubgraph(source, source_sg);

        // see if we have exactly enough elements in the graph
        assertThat(lg.G.getEdges()).hasSize(1);
        assertThat(lg.G.getVertices()).hasSize(2);

        // make sure it is our edge
        assertThatGraphContains(lg.G, edge);
    }


    // add the edge to the subgraph, the other side of the edge will be made symbolic
    protected void addEdgeToSubgraph(MutableSubgraph sg, ID sg_id, EdgeID edge_id)
    {
        final Direction d = GraphUtilities.directionOppositeTo(edge_id, sg_id);
        final ID other = GraphUtilities.getSymbolicID(GraphUtilities.getOppositeId(edge_id, sg_id));
        final Subgraph.Direction dd = d.opposite().equals(Direction.IN) ? Subgraph.Direction.INWARDS : Subgraph.Direction.OUTWARDS;
        sg.beginEdge(edge_id.label(), other, dd);
    }

    protected void assertThatGraphContains(Graph G, EdgeID edge_id)
    {
        final ObjectMapper om = new ObjectMapper();
        final Vertex head = GraphUtilities.findVertex(om, G, edge_id.head());
        final Vertex tail = GraphUtilities.findVertex(om, G, edge_id.tail());
        final Edge edge = GraphUtilities.findEdge(om, G, edge_id);

        Assertions.assertThat(head).isNotNull();
        Assertions.assertThat(tail).isNotNull();
        Assertions.assertThat(edge).isNotNull();

        Assertions.assertThat(edge.getVertex(Direction.IN)).isEqualTo(head);
        Assertions.assertThat(edge.getVertex(Direction.OUT)).isEqualTo(tail);
    }
}