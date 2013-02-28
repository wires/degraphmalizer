package dgm.neo4j;

import com.google.inject.*;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import dgm.graphs.GraphQueries;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class CommonNeo4j extends AbstractModule
{
    @Override
    protected void configure()
    {}

    @Provides @Singleton
    final TransactionalGraph provideGraph(@Neo4jDataDir String dataDir) throws IOException
    {
        // manually set the cache provider
        final Map<String, String> settings = new HashMap<String, String>();
        settings.put("cache_type", "soft");

        final Neo4jGraph graph = new Neo4jGraph(dataDir, settings);

        // quickly get vertices by ID
        final String[] props = new String[] {
                GraphQueries.OWNER, GraphQueries.SYMBOLIC_OWNER,
                GraphQueries.IDENTIFIER, GraphQueries.SYMBOLIC_IDENTIFER };

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