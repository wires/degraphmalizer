package dgm.degraphmalizr.test;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import dgm.Degraphmalizr;
import dgm.GraphUtilities;
import dgm.ID;
import dgm.degraphmalizr.degraphmalize.*;
import dgm.degraphmalizr.recompute.RecomputeResult;
import dgm.exceptions.DegraphmalizerException;
import dgm.modules.BlueprintsSubgraphManagerModule;
import dgm.modules.DegraphmalizerModule;
import dgm.modules.StaticJSConfModule;
import dgm.modules.ThreadpoolModule;
import dgm.modules.elasticsearch.EmphemeralES;
import dgm.modules.neo4j.CommonNeo4j;
import dgm.modules.neo4j.EphemeralEmbeddedNeo4J;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.nnsoft.guice.sli4j.slf4j.Slf4jLoggingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.fest.assertions.Assertions.assertThat;

class LocalNode
{
    private static void setLogLevel(String logger, Level level)
    {
        ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(logger)).setLevel(level);
    }

    public static LocalNode localNode()
    {
        setLogLevel("org.elasticsearch", Level.WARN);
        setLogLevel("dgm", Level.TRACE);

        final ArrayList<Module> modules = new ArrayList<Module>();

        // some defaults
        modules.add(new BlueprintsSubgraphManagerModule());
        modules.add(new DegraphmalizerModule());
        modules.add(new ThreadpoolModule());
        modules.add(new EmphemeralES());
        modules.add(new CommonNeo4j());
        modules.add(new EphemeralEmbeddedNeo4J());
        modules.add(new StaticJSConfModule("src/test/resources/conf/"));
        modules.add(new Slf4jLoggingModule());

        // the injector
        final Injector injector = com.google.inject.Guice.createInjector(modules);
        
        return injector.getInstance(LocalNode.class);
    }

    @InjectLogger
    Logger log;

    @Inject
    Degraphmalizr d;

    @Inject
    Client es;

    @Inject
    Graph G;

    final DegraphmalizeCallback callback = new Fixme();

    class Fixme implements DegraphmalizeCallback
    {

        @Override
        public void complete(DegraphmalizeResult result)
        {
            log.info("complete");
        }

        @Override
        public void started(DegraphmalizeRequest request)
        {
            log.info("started");
        }

        @Override
        public void failed(DegraphmalizerException exception) {
            log.info("failed");
        }
    }
}

@Test
public class DegraphmalizerTest
{
    LocalNode ln;

	@BeforeTest
	public void setUp()
	{
        ln = LocalNode.localNode();
	}

    @AfterTest
    public void shutDown() {
        ln.es.close();
        ln.G.shutdown();
    }

    @Test
	public void fullTest() throws ExecutionException, InterruptedException, DegraphmalizerException
    {
        final String target = "test-target";
        final String idx = "test-index";
        final String tp = "test-type";
        final String id = "1234";

        // create target index if it doesn't exist
        if (!ln.es.admin().indices().prepareExists(target).execute().actionGet().exists())
        {
            final CreateIndexResponse cir = ln.es.admin().indices().prepareCreate(target).execute().actionGet();
            if (! cir.acknowledged())
                throw new RuntimeException("failed to create index " + target);
        }

        final IndexResponse ir = ln.es.prepareIndex(idx,tp,id)
                .setSource("{\"children\":[1,2,3]}").execute().actionGet();

        ln.log.info("Indexed /{}/{}/{} as version {} into ES", new Object[]{idx, tp, id, ir.version()});

        final IndexResponse ir1 = ln.es.prepareIndex(idx,tp,"1")
                .setSource("{\"cheese\":\"gorgonzola\"}").execute().actionGet();

        ln.log.info("Indexed /{}/{}/1 as version {} into ES", new Object[]{idx, tp, ir1.version()});

        final IndexResponse ir2 = ln.es.prepareIndex(idx,tp,"2")
                .setSource("{\"cheese\":\"mozarella\"}").execute().actionGet();

        ln.log.info("Indexed /{}/{}/2 as version {} into ES", new Object[]{idx, tp, ir2.version()});

        // degraphmalize "1" and wait for and print result
        final List<Future<DegraphmalizeResult>> actions = new ArrayList<Future<DegraphmalizeResult>>();

        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.UPDATE, DegraphmalizeRequestScope.DOCUMENT, new ID(idx, tp, id, ir.version()), ln.callback));
        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.UPDATE, DegraphmalizeRequestScope.DOCUMENT, new ID(idx, tp, "1", ir1.version()), ln.callback));
        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.UPDATE, DegraphmalizeRequestScope.DOCUMENT, new ID(idx, tp, "2", ir2.version()), ln.callback));

        for(final Future<DegraphmalizeResult> a : actions)
        {
            DegraphmalizeResult result = a.get();
            ln.log.info("Degraphmalize complete for : " + result.root());
        }

        ObjectMapper mapper = new ObjectMapper();
        for(final Future<DegraphmalizeResult> a : actions)
        {
            final DegraphmalizeResult degraphmalizeResult = a.get();
            // Get first node in results
            final ObjectNode result = toJSON(mapper, degraphmalizeResult.results().get(0).get());

            assertThat(result.get("properties").has("nodes-in")).isTrue();
            assertThat(result.get("properties").has("nodes-out")).isTrue();

            if(degraphmalizeResult.root().id().equals("1234"))
            {
                assertThat(numberOfChildren(result, "nodes-out")).isZero();
                assertThat(numberOfChildren(result, "nodes-in")).isEqualTo(3);
            }

            if(degraphmalizeResult.root().id().equals("1"))
            {
                assertThat(numberOfChildren(result, "nodes-out")).isEqualTo(1);
                assertThat(numberOfChildren(result, "nodes-in")).isZero();
            }
        }

        GraphUtilities.dumpGraph(new ObjectMapper(), ln.G);
        // Cleanup index
        if (!ln.es.admin().indices().delete(new DeleteIndexRequest(idx)).actionGet().acknowledged()) {
            throw new RuntimeException("failed to delete index " + target);
        }
	}

    @Test
    public void deleteTest() throws ExecutionException, InterruptedException {
        final String target = "test-target";
        final String idx = "test-index";
        final String tp = "test-type";
        final String id = "1234";
        final String othertp = "othertest-type";

        // create target index if it doesn't exist
        if (!ln.es.admin().indices().prepareExists(target).execute().actionGet().exists())
        {
            final CreateIndexResponse cir = ln.es.admin().indices().prepareCreate(target).execute().actionGet();
            if (! cir.acknowledged())
                throw new RuntimeException("failed to create index " + target);
        }

        final IndexResponse ir = ln.es.prepareIndex(idx,tp,id)
                .setSource("{\"children\":[1,2,3]}").execute().actionGet();

        ln.log.info("Indexed /{}/{}/{} as version {} into ES", new Object[]{idx, tp, id, ir.version()});

        final IndexResponse ir1 = ln.es.prepareIndex(idx,tp,"1")
                .setSource("{\"cheese\":\"gorgonzola\"}").execute().actionGet();

        ln.log.info("Indexed /{}/{}/1 as version {} into ES", new Object[]{idx, tp, ir1.version()});

        final IndexResponse ir2 = ln.es.prepareIndex(idx,tp,"2")
                .setSource("{\"cheese\":\"mozarella\"}").execute().actionGet();

        ln.log.info("Indexed /{}/{}/2 as version {} into ES", new Object[]{idx, tp, ir2.version()});

        final IndexResponse ir3 = ln.es.prepareIndex(idx,othertp,"3")
                .setSource("{\"cheese\":\"limburger\"}").execute().actionGet();

        ln.log.info("Indexed /{}/{}/3 as version {} into ES", new Object[]{idx, othertp, ir3.version()});

        final List<Future<DegraphmalizeResult>> actions = new ArrayList<Future<DegraphmalizeResult>>();

        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.UPDATE, DegraphmalizeRequestScope.DOCUMENT, new ID(idx, tp, id, ir.version()), ln.callback));
        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.UPDATE, DegraphmalizeRequestScope.DOCUMENT, new ID(idx, tp, "1", ir1.version()), ln.callback));
        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.UPDATE, DegraphmalizeRequestScope.DOCUMENT, new ID(idx, tp, "2", ir2.version()), ln.callback));
        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.UPDATE, DegraphmalizeRequestScope.DOCUMENT, new ID(idx, othertp, "3", ir3.version()), ln.callback));


        for(final Future<DegraphmalizeResult> a : actions)
        {
            DegraphmalizeResult result = a.get();
            ln.log.info("Degraphmalize complete for : "+result.root());
        }

        GraphUtilities.dumpGraph(new ObjectMapper(), ln.G);

        actions.clear();
        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.DELETE, DegraphmalizeRequestScope.DOCUMENT_ANY_VERSION, new ID(idx, tp, id, 0), ln.callback));
        for(final Future<DegraphmalizeResult> a : actions)
        {
            DegraphmalizeResult result = a.get();
            ln.log.info("Degraphmalize of {}: {}", result.root(), result);
        }

        actions.clear();
        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.DELETE, DegraphmalizeRequestScope.TYPE_IN_INDEX, new ID(idx, tp, null, 0), ln.callback));
        for(final Future<DegraphmalizeResult> a : actions)
        {
            DegraphmalizeResult result = a.get();
            ln.log.info("Degraphmalize of {}: {}", result.root(), result);
        }

        // Only the vertex of the othertp type should be present.
        Iterable<Vertex> iterable = GraphUtilities.findVerticesInIndex(ln.G, idx);
        assertThat(countIterator(iterable)).isEqualTo(1);

        actions.clear();
        actions.add(ln.d.degraphmalize(DegraphmalizeRequestType.DELETE, DegraphmalizeRequestScope.INDEX, new ID(idx, null, null, 0), ln.callback));
        for(final Future<DegraphmalizeResult> a : actions)
        {
            DegraphmalizeResult result = a.get();
            ln.log.info("Degraphmalize of {}: {}", result.root(), result);
        }

        // No more vertices
        Iterable<Vertex> iterable2 = GraphUtilities.findVerticesInIndex(ln.G, idx);
        assertThat(countIterator(iterable2)).isEqualTo(0);

        // Cleanup index
        if (!ln.es.admin().indices().delete(new DeleteIndexRequest(idx)).actionGet().acknowledged()) {
            throw new RuntimeException("failed to delete index " + target);
        }
    }

    private int countIterator(Iterable<Vertex> iterable)
    {
        int count=0;
        for (Vertex vertex : iterable) {
            count++;
        }
        return count;
    }


    private int numberOfChildren(JsonNode result, String property)
    {
        return result.get("properties").get(property).get("full_tree").get("_children").size();
    }

    public static ObjectNode toJSON(ObjectMapper objectMapper, RecomputeResult success) throws InterruptedException, ExecutionException
    {
        final ObjectNode n = objectMapper.createObjectNode();

        // write targetID using index reponse
        final IndexResponse ir = success.indexResponse();
        final ObjectNode targetID = objectMapper.createObjectNode();
        targetID.put("index", ir.index());
        targetID.put("type", ir.type());
        targetID.put("id", ir.id());
        targetID.put("version", ir.version());
        n.put("targetID", targetID);

        // write dictionary of properties and their values
        final ObjectNode properties = objectMapper.createObjectNode();
        for(Map.Entry<String,JsonNode> entry : success.properties().entrySet())
            properties.put(entry.getKey(), entry.getValue());
        n.put("properties", properties);

        // write dictionary of properties and their values
        n.put("sourceDocumentAfterTransform", success.sourceDocument());

        return n;
   }
}

