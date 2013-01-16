package vpro;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.node.Node;
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

    @BeforeClass
    public void setupServer()
    {
        node = nodeBuilder().local(true).settings(settingsBuilder()
                .put("path.data", "target/data")
                .put("cluster.name", "test-cluster-" + NetworkUtils.getLocalAddress())
                .put("gateway.type", "none")).node();
    }

    @AfterClass
    public void closeServer()
    {
        node.close();
    }

    @BeforeMethod
    public void createIndex()
    {
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
        node.stop();
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
}
