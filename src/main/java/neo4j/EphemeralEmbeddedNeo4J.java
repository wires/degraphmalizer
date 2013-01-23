package neo4j;

import com.google.inject.*;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import graphs.GraphQueries;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class EphemeralEmbeddedNeo4J extends AbstractModule
{
	@Provides
	@Singleton
	TransactionalGraph provideGraph(@Neo4jDataDir String dataDir) throws IOException
    {
        FileUtils.deleteDirectory(new File(dataDir));

        // manually set the cache provider
        final Map<String, String> settings = new HashMap<String, String>();
        settings.put("cache_type", "soft");

        final Neo4jGraph graph = new Neo4jGraph(dataDir, settings);
		
		// quickly get vertices by ID
		graph.createKeyIndex(GraphQueries.IDENTIFIER, Vertex.class);
		
		return graph;
	}
	
	@Override
	protected void configure()
	{}
}