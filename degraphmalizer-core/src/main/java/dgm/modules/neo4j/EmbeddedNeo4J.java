package dgm.modules.neo4j;

import com.google.inject.AbstractModule;

public class EmbeddedNeo4J extends AbstractModule
{
    final String dataDir;

    public EmbeddedNeo4J(String dataDir)
    {
        this.dataDir = dataDir;
    }

    @Override
    protected final void configure()
    {
        bind(String.class).annotatedWith(Neo4jDataDir.class).toInstance(dataDir);
    }
}
