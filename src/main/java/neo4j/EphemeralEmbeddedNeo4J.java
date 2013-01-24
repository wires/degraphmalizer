package neo4j;

import com.google.common.io.Files;
import com.google.inject.*;

import java.io.File;

public class EphemeralEmbeddedNeo4J extends AbstractModule
{
    @Override
    protected final void configure()
    {
        // create temporary directory and use this as Neo4j datadir
        final File dataDir = Files.createTempDir();

        bind(String.class).annotatedWith(Neo4jDataDir.class).toInstance(dataDir.getAbsolutePath());
    }
}