package dgm.degraphmalizr.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.*;
import dgm.EdgeID;
import dgm.ID;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeAction;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeActionScope;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeActionType;
import dgm.exceptions.DegraphmalizerException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static dgm.GraphUtilities.getEdgeID;
import static org.fest.assertions.Assertions.assertThat;

@Test
public class ConfigExtractMergingTest
{
    LocalNode ln;

	@BeforeClass
	public void setUp()
	{
        ln = LocalNode.localNode();
        for(Vertex v: ln.G.getVertices())
            ln.G.removeVertex(v);
        ((TransactionalGraph)ln.G).stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
	}

    IndexResponse index(String idx, String tp, String id)
    {
        final IndexResponse ir = ln.es.prepareIndex(idx,tp,id)
                .setSource("{\"identifier\":\"" + idx + "," + tp + "," + id + "\"}").execute().actionGet();

        ln.log.info("Indexed /{}/{}/{} as version {} into ES", new Object[]{idx, tp, id, ir.version()});

        return ir;
    }

    void createIndex(String idx)
    {
        // create target index if it doesn't exist
        if (!ln.es.admin().indices().prepareExists(idx).execute().actionGet().exists())
        {
            final CreateIndexResponse cir = ln.es.admin().indices().prepareCreate(idx).execute().actionGet();
            if (! cir.acknowledged())
                throw new RuntimeException("failed to create index " + idx);
        }
    }

    void dumpGraph()
    {
        for(Vertex v : ln.G.getVertices())
        {
            System.err.print("v[" + v.getId() + "]={");
            for(String k : v.getPropertyKeys())
                System.err.print("\n\t" + k + ": " + v.getProperty(k));
            System.err.println("}");
        }

        final ObjectMapper om = new ObjectMapper();
        for(Edge e : ln.G.getEdges())
        {
            final EdgeID edgeId = getEdgeID(om, e);
            if(edgeId == null)
                System.err.println(e.toString());
            else
                System.err.println(edgeId.toString());
        }
    }

    @Test
	public void mergeExtractFunctionsTest() throws ExecutionException, InterruptedException, DegraphmalizerException
    {
        assertThat(ln.G.getEdges()).hasSize(0);
        assertThat(ln.G.getVertices()).hasSize(0);

        final String src = "test-merge-multiple-src";
        final String dest = "test-merge-multiple-dest";
        final String type = "test-type";

        createIndex(src);
        createIndex(dest);

        final long v1 = index(src, type, "1").version();
        final long v2 = index(src, type, "2").version();

        // degraphmalize "1" and wait for and print result
        final ArrayList<DegraphmalizeAction> actions = new ArrayList<DegraphmalizeAction>();
        actions.add(ln.d.degraphmalize(DegraphmalizeActionType.UPDATE, DegraphmalizeActionScope.DOCUMENT, new ID(src, type, "1", v1), ln.callback));
        actions.add(ln.d.degraphmalize(DegraphmalizeActionType.UPDATE, DegraphmalizeActionScope.DOCUMENT, new ID(src, type, "2", v2), ln.callback));

        for(final DegraphmalizeAction a : actions)
            ln.log.info("Degraphmalize of {} completed: {}", a.id(), a.resultDocument().get());

        dumpGraph();

        assertThat(ln.G.getEdges()).hasSize(4);
        assertThat(ln.G.getVertices()).hasSize(4);

        ln.es.close();
        ln.G.shutdown();
	}
}
