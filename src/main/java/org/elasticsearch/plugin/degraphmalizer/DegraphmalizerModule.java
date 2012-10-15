package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.common.inject.AbstractModule;

/**
 * Created with IntelliJ IDEA.
 * User: wires
 * Date: 10/15/12
 * Time: 7:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected void configure() {
        bind(DegraphmalizerListener.class).asEagerSingleton();
    }
}
