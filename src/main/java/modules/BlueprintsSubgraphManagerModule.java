package modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import com.tinkerpop.blueprints.TransactionalGraph;
import graphs.ops.BlueprintsSubgraphManager;
import graphs.ops.SubgraphManager;

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
