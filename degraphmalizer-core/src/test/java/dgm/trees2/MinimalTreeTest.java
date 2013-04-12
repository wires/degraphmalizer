package dgm.trees2;

import com.google.common.base.Function;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import dgm.trees.*;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;


@Test
public class MinimalTreeTest
{
    // complete graph on 3 vertices, but then directed
    public static Pair<Graph,Vertex> K_3()
    {
        final TinkerGraph g = new TinkerGraph();

        final Vertex v1 = g.addVertex(null);
        final Vertex v2 = g.addVertex(null);
        final Vertex v3 = g.addVertex(null);

        final Edge e1 = g.addEdge(null, v1, v2, "this");
        final Edge e2 = g.addEdge(null, v2, v3, "is");
        final Edge e3 = g.addEdge(null, v3, v1, "cyclic");

        return new Pair<Graph,Vertex>(g,v1);
    }

    /* This graph:
     *
     * <pre>
     *
     *      ,-->(2)
     *    /
     * (1)---->(3)
     *   \
     *    `--->(4)
     *
     * </pre>
     */
    public static Pair<Graph,Vertex> forkGraph()
    {
        final TinkerGraph g = new TinkerGraph();

        final Vertex v1 = g.addVertex(null);
        final Vertex v2 = g.addVertex(null);
        final Vertex v3 = g.addVertex(null);
        final Vertex v4 = g.addVertex(null);

        final Edge e1 = g.addEdge(null, v1, v2, "a");
        final Edge e2 = g.addEdge(null, v1, v3, "b");
        final Edge e3 = g.addEdge(null, v1, v4, "c");

        return new Pair<Graph,Vertex>(g,v1);
    }

    final static String nullSafeToString(final Object o)
    {
        if(o == null)
            return "null";

        return o.toString();
    }

    final static Function<Pair<Edge,Vertex>,String> show = new Function<Pair<Edge, Vertex>, String>()
    {
        @Override
        public String apply(Pair<Edge, Vertex> input)
        {
            final StringBuilder sb = new StringBuilder("(");
            sb.append(nullSafeToString(input.a));
            sb.append("--");
            sb.append(nullSafeToString(input.b));
            sb.append(')');
            return sb.toString();
        }
    };


    @Test
    public void testTreeVisitors()
    {
        // 1 --> 2 --> 3 --> 1 --> 2 --> ...
        final Pair<Graph,Vertex> p = K_3();
        final Vertex root = p.b;

        final TreeViewer<Pair<Edge, Vertex>> tv = new GraphTreeViewer(Direction.OUT);

        final PrettyPrinter<Pair<Edge,Vertex>> pp = new PrettyPrinter<Pair<Edge, Vertex>>(show);
        final LevelLimitingVisitor<Pair<Edge,Vertex>> llpp = new LevelLimitingVisitor<Pair<Edge, Vertex>>(5, pp);

        Trees2.bfsVisit(new Pair<Edge, Vertex>(null, root), tv, llpp);

        final String s = "(null--v[0])\n" +
                "  (e[3][0-this->1]--v[1])\n" +
                "    (e[4][1-is->2]--v[2])\n" +
                "      (e[5][2-cyclic->0]--v[0])\n" +
                "        (e[3][0-this->1]--v[1])\n";

        assertThat(s).isEqualTo(pp.toString());


        final PrettyPrinter<Pair<Edge,Vertex>> pp2 = new PrettyPrinter<Pair<Edge, Vertex>>(show);
        final OccurrenceTracker<Pair<Edge,Vertex>> ot = new NodeAlreadyVisitedTracker();
        final CycleKiller<Pair<Edge,Vertex>> ckpp = new CycleKiller<Pair<Edge, Vertex>>(pp2, ot);

        Trees2.bfsVisit(new Pair<Edge, Vertex>(null, root), tv, ckpp);

        final String t = "(null--v[0])\n" +
                "  (e[3][0-this->1]--v[1])\n" +
                "    (e[4][1-is->2]--v[2])\n";

        assertThat(t).isEqualTo(pp2.toString());
    }

    @Test
    public void testTreeBuilder()
    {
        final Pair<Graph,Vertex> p = forkGraph();
        final TreeViewer<Pair<Edge, Vertex>> tv = new GraphTreeViewer(Direction.OUT);
        final TreeBuilder<Pair<Edge,Vertex>> tb = new TreeBuilder<Pair<Edge, Vertex>>();

        Trees2.bfsVisit(new Pair<Edge,Vertex>(null, p.b), tv, tb);

        final Tree<Pair<Edge,Vertex>> tree = tb.tree();

        assertThat(tree.children()).hasSize(3);
    }
}
