/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.plugin.degraphmalizer.updater.UpdaterManager;

import java.util.List;

/**
 * User: rico
 * Date: 11/03/2013
 */
public class DegraphmalizerClusterListener implements ClusterStateListener {
    ClusterService clusterService;
    UpdaterManager updaterManager;
    private static final String DEGRAPHMALIZER_NODENAME = "Degraphmalizer";

    @Inject
    public DegraphmalizerClusterListener(ClusterService clusterService, UpdaterManager updaterManager) {
        this.clusterService = clusterService;
        this.updaterManager = updaterManager;
        clusterService.add(this);
    }

    @Override
    public void clusterChanged(ClusterChangedEvent clusterChangedEvent) {
        if (clusterChangedEvent.nodesChanged()) {
            DiscoveryNodes.Delta nodesChanged = clusterChangedEvent.nodesDelta();
            if (nodesChanged.added()) {
                if (findNode(nodesChanged.addedNodes(), DEGRAPHMALIZER_NODENAME) != null) {
                    updaterManager.startSending();
                }
            }
            if (nodesChanged.removed()) {
                if (findNode(nodesChanged.removedNodes(), DEGRAPHMALIZER_NODENAME) != null) {
                    updaterManager.stopSending();
                }
            }
        }
    }

    private DiscoveryNode findNode(List<DiscoveryNode> nodes, String nodeName) {
        for (DiscoveryNode node : nodes) {
            if (DEGRAPHMALIZER_NODENAME.equals(node.name())) {
                return node;
            }
        }
        return null;
    }
}
