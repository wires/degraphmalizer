package degraphmalizr;

import com.google.inject.*;
import com.tinkerpop.blueprints.TransactionalGraph;
import graphs.ops.BlueprintsSubgraphManager;
import graphs.ops.SubgraphManager;

public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(Degraphmalizr.class).to(Degraphmalizer.class).asEagerSingleton();
    }
}
