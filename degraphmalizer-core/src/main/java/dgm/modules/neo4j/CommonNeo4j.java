package dgm.modules.neo4j;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import dgm.GraphUtilities;
import dgm.Service;
import dgm.modules.ServiceModule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


class Neo4jService implements Service
{
    final Graph graph;

    @Inject
    Neo4jService(Graph graph)
    {
        this.graph = graph;
    }

    @Override
    public void start()
    {}

    @Override
    public void stop()
    {
        graph.shutdown();
    }
}


public class CommonNeo4j extends ServiceModule
{
    @Override
    protected void configure()
    {
        bindService(Neo4jService.class);
    }

    @Provides @Singleton
    final TransactionalGraph provideGraph(@Neo4jDataDir String dataDir) throws IOException
    {
        // manually set the cache provider
        final Map<String, String> settings = new HashMap<String, String>();
        settings.put("cache_type", "soft");

        final Neo4jGraph graph = new Neo4jGraph(dataDir, settings);

        // quickly get vertices by ID
        final String[] props = new String[] {
                GraphUtilities.OWNER, GraphUtilities.SYMBOLIC_OWNER,
                GraphUtilities.IDENTIFIER, GraphUtilities.SYMBOLIC_IDENTIFER,
                GraphUtilities.KEY_INDEX, GraphUtilities.KEY_TYPE,
                GraphUtilities.KEY_ID, GraphUtilities.KEY_VERSION };

        for(String prop : props)
        {
            graph.createKeyIndex(prop, Vertex.class);
            graph.createKeyIndex(prop, Edge.class);
        }

        graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);

        return graph;
    }

    @Provides @Singleton
    final Graph provideGr(TransactionalGraph graph)
    {
        return graph;
    }
}