package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Singleton;

public class GraphUpdaterModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GraphUpdater.class).to(GraphUpdaterImpl.class).in(Singleton.class);
    }
}
