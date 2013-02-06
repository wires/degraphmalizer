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
public class UpdaterManager extends AbstractLifecycleComponent<UpdaterManager> implements UpdaterManagerMBean {
    private static final ESLogger LOG = Loggers.getLogger(UpdaterManager.class);

    private Map<String, Updater> updaters = new HashMap<String, Updater>();

    private final String uriScheme;
    private final String uriHost;
    private final int uriPort;
    private final long retryDelayOnFailureInMillis;

    private int queueLimit;
    private String logPath;
    private int maxRetries;

    @Inject
    public UpdaterManager(final Settings settings) {
        super(settings);

        final Settings pluginSettings = settings.getComponentSettings(DegraphmalizerPlugin.class);

        // Please keep this in sync with the documentation in README.md
        this.uriScheme = pluginSettings.get("DegraphmalizerPlugin.degraphmalizerScheme", "http");
        this.uriHost = pluginSettings.get("DegraphmalizerPlugin.degraphmalizerHost", "localhost");
        this.uriPort = pluginSettings.getAsInt("DegraphmalizerPlugin.degraphmalizerPort", 9821);
        this.retryDelayOnFailureInMillis = pluginSettings.getAsLong("DegraphmalizerPlugin.retryDelayOnFailureInMillis", 10000l);

        this.queueLimit = pluginSettings.getAsInt("DegraphmalizerPlugin.queueLimit", 100000);
        this.logPath = pluginSettings.get("DegraphmalizerPlugin.logPath", "/export/elasticsearch/degraphmalizer");
        this.maxRetries = pluginSettings.getAsInt("DegraphmalizerPlugin.maxRetries", 10);
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        registerMBean();
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        for (Map.Entry<String, Updater> entry : updaters.entrySet()) {
            LOG.info("Shutting down updater for index " + entry.getKey());
            entry.getValue().shutdown();
        }
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    public void startUpdater(final String index) {
        if (updaters.containsKey(index)) {
            LOG.warn("Updater for index {} already exists", index);
            return;
        }
        final Updater updater = new Updater(index, uriScheme, uriHost, uriPort, retryDelayOnFailureInMillis, logPath, queueLimit, maxRetries);
        updaters.put(index, updater);
        updater.start();
        LOG.info("Updater started for index {}", index);
    }

    public void stopUpdater(final String index) {
        final Updater updater = updaters.get(index);
        if (updater != null) {
            updater.shutdown();
            updaters.remove(index);
            LOG.info("Updater stopped for index {}", index);
        } else {
            LOG.warn("No updater found for index {}", index);
        }
    }

    public void add(final String index, final Change change) {
        final Updater updater = updaters.get(index);
        if (updater != null) {
            updater.add(change);
        } else {
            LOG.error("There is no updater for index {}, dropping change {}", index, change);
        }
    }

    @Override
    public Map<String, Integer> getQueueSizes() {
        final Map<String, Integer> indexQueueSizes = new HashMap<String, Integer>(updaters.size());
        for (Map.Entry<String, Updater> entry : updaters.entrySet()) {
            indexQueueSizes.put(entry.getKey(), entry.getValue().getQueueSize());
        }
        return indexQueueSizes;
    }

    @Override
    public boolean flushQueue(final String indexName) {
        final Updater updater = updaters.get(indexName);
        if (updater != null) {
            LOG.info("Flushing queue for index {} with {} entries", indexName, updater.getQueueSize());
            updater.flushQueue();
            return true;
        }
        return false;
    }

    private void registerMBean() {
        try {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName("org.elasticsearch.plugin.degraphmalizer.UpdaterManager:type=UpdaterManager");
            mbs.registerMBean(this, name);
            LOG.info("Registered MBean");
        } catch (Exception e) {
            LOG.error("Failed to register MBean", e);
        }
    }
}
