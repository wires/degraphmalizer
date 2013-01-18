package org.elasticsearch.plugin.degraphmalizer;

/**
 * MBean interface for JMX monitoring.
 */
public interface GraphUpdaterMBean
{
    public int getQueueSize();
}
