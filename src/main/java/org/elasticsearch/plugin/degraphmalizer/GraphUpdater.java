package org.elasticsearch.plugin.degraphmalizer;

public interface GraphUpdater
{
    public void add(GraphChange change);

    public void shutdown();
}
