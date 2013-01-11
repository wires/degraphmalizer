package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.AbstractModule;

public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(DegraphmalizerListener.class).asEagerSingleton();
    }
}
