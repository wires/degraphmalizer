package org.elasticsearch.plugin.degraphmalizer;

import java.util.Map;

/**
 * MBean interface for JMX monitoring.
 */
public interface UpdaterManagerMBean {
    public Map<String, Integer> getQueueSizes();

    public boolean flushQueue(String indexName);
}
