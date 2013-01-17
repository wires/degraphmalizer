package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.*;

public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(GraphUpdater.class).in(Singleton.class);
        bind(DegraphmalizerLifecycleListener.class).asEagerSingleton();
    }
}
