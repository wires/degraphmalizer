package modules;

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
    SubgraphManager provideSubgraphManager(TransactionalGraph G)
    {
        return new BlueprintsSubgraphManager(G);
    }
}
