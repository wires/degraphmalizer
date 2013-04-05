package dgm.modules.elasticsearch.nodes;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * Configure ES node that joins the cluster, no data etc.
 */
public class NodeES extends AbstractModule
{
    protected final String cluster;
    protected final String host;
    protected final int port;

    public NodeES(String cluster, String host, int port)
    {
        this.cluster = cluster;
        this.host = host;
        this.port = port;
    }

    @Override
    protected void configure()
    {}

    @Provides
    @Singleton
    final Node provideElasticInterface()
    {
        // otherwise run as much as possible in memory
        final Settings.Builder settings = ImmutableSettings.settingsBuilder()
                .put("node.name", "Degraphmalizer")
                .put("node.data",false)
                .put("node.client",true)
                .put("node.http.enabled", true)
                .put("cluster.name", cluster)
                .put("discovery.zen.ping.multicast.ttl", 4)
                .put("discovery.zen.ping.multicast.enabled", true)
                .put("discovery.zen.ping.multicast.ping.enabled", false)
                .put("discovery.zen.ping.unicast.hosts", host + ":" + port)
                .put("client.transport.sniff", true);

        return NodeBuilder.nodeBuilder().settings(settings).build();
    }
}