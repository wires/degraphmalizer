package dgm.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import com.tinkerpop.blueprints.TransactionalGraph;
import dgm.graphs.BlueprintsSubgraphManager;
import dgm.SubgraphManager;

public class BlueprintsSubgraphManagerModule extends AbstractModule
{
    @Override
    protected void configure()
    {}

    @Provides @Inject @Singleton
    final SubgraphManager provideSubgraphManager(ObjectMapper om, TransactionalGraph G)
    {
        return new BlueprintsSubgraphManager(om, G);
    }
}
