/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * User: rico
 * Date: 29/01/2013
 */
public class GraphUpdaterManager extends AbstractLifecycleComponent<GraphUpdaterManager> implements GraphUpdaterManagerMBean {
    private static final ESLogger LOG = Loggers.getLogger(GraphUpdaterManager.class);

    private Map<String,GraphUpdater> graphUpdaters = new HashMap<String,GraphUpdater>();

    private final String uriScheme;
    private final String uriHost;
    private final int uriPort;
    private final long retryDelayOnFailureInMillis;

    @Inject
    public GraphUpdaterManager(Settings settings) {
        super(settings);

        final Settings pluginSettings = settings.getComponentSettings(DegraphmalizerPlugin.class);

        // Please keep this in sync with the documentation in README.md
        this.uriScheme = pluginSettings.get("DegraphmalizerPlugin.degraphmalizerScheme", "http");
        this.uriHost = pluginSettings.get("DegraphmalizerPlugin.degraphmalizerHost", "localhost");
        this.uriPort = pluginSettings.getAsInt("DegraphmalizerPlugin.degraphmalizerPort", 9821);
        this.retryDelayOnFailureInMillis = pluginSettings.getAsLong("DegraphmalizerPlugin.retryDelayOnFailureInMillis", 10000l);

    }

    @Override
    protected void doStart() throws ElasticSearchException {
        registerMBean();
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        for (Map.Entry<String, GraphUpdater> entry : graphUpdaters.entrySet()) {
            LOG.info("Shutting down updater for index "+entry.getKey());
            entry.getValue().shutdown();
        }
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    public void startGraphUpdater(String index) {
        if (graphUpdaters.containsKey(index)) {
            LOG.warn("Graph updater for index {} already exists",index);
            return;
        }
        GraphUpdater graphUpdater=new GraphUpdater(index,uriScheme,uriHost,uriPort,retryDelayOnFailureInMillis);
        graphUpdaters.put(index,graphUpdater);
        graphUpdater.start();
    }

    public void stopGraphUpdater(String index) {
        GraphUpdater graphUpdater=graphUpdaters.get(index);
        if (graphUpdater!=null) {
            graphUpdater.shutdown();
            graphUpdaters.remove(index);
        } else {
            LOG.warn("No graph updater found for index {}",index);
        }
    }

    public void add(String index, final GraphChange change) {
        GraphUpdater graphUpdater=graphUpdaters.get(index);
        if (graphUpdater!=null) {
            graphUpdater.add(change);
        } else {
            LOG.error("There is no updater for index {}, dropping change {}", index, change);
        }
    }


    @Override
    public Map<String, Integer> getQueueSizes() {
        Map<String, Integer> indexQueueSizes = new HashMap<String, Integer>(graphUpdaters.size());
        for (Map.Entry<String, GraphUpdater> entry : graphUpdaters.entrySet()) {
            indexQueueSizes.put(entry.getKey(),entry.getValue().getQueueSize());
        }
        return indexQueueSizes;
    }

    @Override
    public boolean flushQueue(String indexName) {
        GraphUpdater graphUpdater=graphUpdaters.get(indexName);
        if (graphUpdater!=null) {
            LOG.info("Flushing queue for index {} with {} entries ",indexName, graphUpdater.getQueueSize());
            graphUpdater.flushQueue();
            return true;
        }
        return false;
    }

    private void registerMBean()
    {
        try
        {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName("org.elasticsearch.plugin.degraphmalizer.GraphUpdaterManager:type=GraphUpdaterManager");
            mbs.registerMBean(this, name);
            LOG.info("Registered MBean");
        } catch (Exception e) {
            LOG.error("Failed to register MBean", e);
        }
    }
}
