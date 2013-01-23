package neo4j;

import com.google.inject.*;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;


public class CommonNeo4j extends AbstractModule
{
	@Provides
	@Singleton
	Graph provideGr(TransactionalGraph graph)
	{
		return graph;
	}

	@Override
    protected void configure()
    {
	    bind(String.class).annotatedWith(Neo4jDataDir.class).toInstance("data/graph/");
    }

}
