/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package org.elasticsearch.plugin.degraphmalizer.updater;

/**
 * MBean interface for JMX monitoring.
 */
public interface UpdaterQueueMBean {
    int size();
    int getInputQueueSize();
    int getOutputQueueSize();
    int getOverflowSize();
    String getIndex();
    void clear();
}
