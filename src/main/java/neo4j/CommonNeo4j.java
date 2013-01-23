package neo4j;

import com.google.inject.*;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import graphs.GraphQueries;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class CommonNeo4j extends AbstractModule
{
    @Override
    protected void configure()
    {}

    @Provides @Singleton
    TransactionalGraph provideGraph(@Neo4jDataDir String dataDir) throws IOException
    {
        // manually set the cache provider
        final Map<String, String> settings = new HashMap<String, String>();
        settings.put("cache_type", "soft");

        final Neo4jGraph graph = new Neo4jGraph(dataDir, settings);

        // quickly get vertices by ID
        graph.createKeyIndex(GraphQueries.IDENTIFIER, Vertex.class);
        graph.createKeyIndex(GraphQueries.SYMBOLIC_OWNER, Vertex.class);
        graph.createKeyIndex(GraphQueries.SYMBOLIC_OWNER, Edge.class);

        return graph;
    }

    @Provides @Singleton
    Graph provideGr(TransactionalGraph graph)
    {
        return graph;
    }
}