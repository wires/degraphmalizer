package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.*;
import org.elasticsearch.common.inject.assistedinject.FactoryProvider;

interface DegraphmalizerIndexListenerFactory
{
    DegraphmalizerIndexListener create(String indexName);
}

public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(GraphUpdater.class).to(GraphUpdaterImpl.class).in(Singleton.class);
        bind(DegraphmalizerLifecycleListener.class).asEagerSingleton();

        bind(DegraphmalizerIndexListenerFactory.class).toProvider(
                FactoryProvider.newFactory(DegraphmalizerIndexListenerFactory.class, DegraphmalizerIndexListener.class));
    }
}
