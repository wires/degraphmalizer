package org.elasticsearch.plugin.degraphmalizer;

public interface DegraphmalizerIndexListenerFactory
{
    DegraphmalizerIndexListener create(String index);
}