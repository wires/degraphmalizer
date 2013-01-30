package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.*;

/**
 * The Google Guice dependency injection module for the Degraphmalizer plugin.
 */
public class DegraphmalizerPluginModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(UpdaterManager.class).in(Singleton.class);
        bind(DegraphmalizerLifecycleListener.class).asEagerSingleton();
    }
}
