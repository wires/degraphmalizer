package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.*;

public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        requireBinding(GraphUpdater.class);
        bind(DegraphmalizerLifecycleListener.class).asEagerSingleton();
    }
}
