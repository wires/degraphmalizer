package degraphmalizr.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.tinkerpop.blueprints.Graph;
import exceptions.DegraphmalizerException;
import modules.*;
import degraphmalizr.*;
import degraphmalizr.jobs.*;
import elasticsearch.LocalES;
import neo4j.CommonNeo4j;
import neo4j.EphemeralEmbeddedNeo4J;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.nnsoft.guice.sli4j.slf4j.Slf4jLoggingModule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

class LocalNode
{
    public static LocalNode localNode()
    {
        final ArrayList<Module> modules = new ArrayList<Module>();

        // some defaults
        modules.add(new BlueprintsSubgraphManagerModule());
        modules.add(new DegraphmalizerModule());
        modules.add(new ThreadpoolModule());
        modules.add(new LocalES());
        modules.add(new CommonNeo4j());
        modules.add(new EphemeralEmbeddedNeo4J());
        modules.add(new StaticJSConfModule("scripts/"));
        modules.add(new Slf4jLoggingModule(Matchers.any()));

        modules.add(new LogconfModule());

        // the injector
        final Injector injector = com.google.inject.Guice.createInjector(modules);

        final LocalNode ln = injector.getInstance(LocalNode.class);

        return ln;
    }

    @Inject
    Degraphmalizr d;

    @Inject
    Client es;

    @Inject
    Graph G;
}

@Test
public class DegraphmalizerTest implements DegraphmalizeStatus
{
    LocalNode ln;

	@BeforeClass
	public void setUp()
	{
        ln = LocalNode.localNode();
	}

    @Test
	public void aTest() throws ExecutionException, InterruptedException, DegraphmalizerException
    {
        final String target = "test-target";
        final String idx = "test-index";
        final String tp = "test-type";
        final String id = "1234";

        // create target index if it doesn't exist
        if (!ln.es.admin().indices().prepareExists(target).execute().actionGet().exists())
        {
            final CreateIndexResponse cir = ln.es.admin().indices().prepareCreate(target).execute().actionGet();
            if (cir.acknowledged() == false)
                throw new RuntimeException("failed to create index " + target);
        }

        final IndexResponse ir = ln.es.prepareIndex(idx,tp,id)
                .setSource("{\"children\":[1,2,3]}").execute().actionGet();

        System.err.println("indexed /" + idx + "/" + tp + "/" + id + " as version " + ir.version() + " into ES");

        final IndexResponse ir1 = ln.es.prepareIndex(idx,tp,"1")
                .setSource("{\"cheese\":\"gorgonzola\"}").execute().actionGet();

        System.err.println("indexed /" + idx + "/" + tp + "/1 as version " + ir1.version() + " into ES");

        final IndexResponse ir2 = ln.es.prepareIndex(idx,tp,"2")
                .setSource("{\"cheese\":\"mozarella\"}").execute().actionGet();

        System.err.println("indexed /" + idx + "/" + tp + "/2 as version " + ir2.version() + " into ES");


        // degraphmalize "1" and wait for and print result
        final DegraphmalizeAction action1 = ln.d.degraphmalize(new ID(idx,tp,"1",ir1.version()), this);
        System.err.println("degramalize of 1: " + action1.resultDocument().get().toString());

        // degraphmalize "2" and wait for and print result
        final DegraphmalizeAction action2 = ln.d.degraphmalize(new ID(idx,tp,"2",ir2.version()), this);
        System.err.println("degramalize of 2: " + action2.resultDocument().get().toString());


        // degraphmalize "1234"
        final DegraphmalizeAction action0 = ln.d.degraphmalize(new ID(idx,tp,id,ir.version()), this);
        System.err.println("degramalize of 3: " + action0.resultDocument().get().toString());

        assertThat(action0.resultDocument().get().get("succes").toString().equals("true")).isTrue();
        assertThat(action1.resultDocument().get().get("succes").toString().equals("true")).isTrue();
        assertThat(action2.resultDocument().get().get("succes").toString().equals("true")).isTrue();

        ln.es.close();
        ln.G.shutdown();
	}

    @Override
    public void recomputeStarted(RecomputeAction action)
    {
        System.err.println("restart");
    }

    @Override
    public void recomputeComplete(RecomputeResult result)
    {
        System.err.println("rcomplete");
    }

    @Override
    public void complete(DegraphmalizeResult result)
    {
        System.err.println("dcomplete");
    }

    @Override
    public void exception(DegraphmalizeResult result)
    {
        System.err.println("exception: " + result.exception().getMessage());
        result.exception().printStackTrace();
    }
}
