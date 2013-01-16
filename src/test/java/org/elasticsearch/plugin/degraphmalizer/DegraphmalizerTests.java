package org.elasticsearch.plugin.degraphmalizer;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugin.degraphmalizer.DegraphmalizerPlugin;
import org.testng.annotations.*;

import java.io.IOException;

import static org.elasticsearch.client.Requests.*;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test
public class DegraphmalizerTests
{
    private final ESLogger logger = Loggers.getLogger(getClass());

    private Node node;

    @BeforeMethod
    public void createIndex()
    {
        node = nodeBuilder().local(true).settings(settingsBuilder()
                .put("path.data", "target/data")
                .put("cluster.name", "test-cluster-" + NetworkUtils.getLocalAddress())
                .put("gateway.type", "none")
                .put("plugin.degraphmalizer.DegraphmalizerPlugin.degraphmalizerHost", "127.0.0.1")).node();

        logger.info("creating index [test]");
        node.client().admin().indices().create(createIndexRequest("test").settings(settingsBuilder().put("index.numberOfReplicas", 0))).actionGet();
        logger.info("Running Cluster Health");
        ClusterHealthResponse clusterHealth = node.client().admin().cluster().health(clusterHealthRequest().waitForGreenStatus()).actionGet();
        logger.info("Done Cluster Health, status " + clusterHealth.status());
        assertThat(clusterHealth.timedOut(), equalTo(false));
        assertThat(clusterHealth.status(), equalTo(ClusterHealthStatus.GREEN));
    }

    @AfterMethod
    public void deleteIndex()
    {
        logger.info("deleting index [test]");
        node.client().admin().indices().delete(deleteIndexRequest("test")).actionGet();
        logger.info("stopping ES");
        node.close();
    }

    @Test
    public void testHookOk() throws IOException
    {
        IndexResponse response = node.client().index(indexRequest("test").type("person").source(
                jsonBuilder().startObject().field("jelle", "was here").endObject())).actionGet();

        String id = response.getId();

        node.client().admin().indices().refresh(refreshRequest()).actionGet();

        node.client().index(indexRequest("test").type("person").source(
                jsonBuilder().startObject().field("jelle", "was here").endObject())).actionGet();

        node.client().delete(deleteRequest("test").type("person").id(id)).actionGet();

        node.client().admin().indices().refresh(refreshRequest()).actionGet();
    }

    @Test
    public void testPluginSettings()
    {
        final Settings pluginSettings = node.settings().getComponentSettings(DegraphmalizerPlugin.class);
        final String host = pluginSettings.get("DegraphmalizerPlugin.degraphmalizerHost");
        assertThat(host, equalTo("127.0.0.1")); // As set in setupServer()
    }
}
