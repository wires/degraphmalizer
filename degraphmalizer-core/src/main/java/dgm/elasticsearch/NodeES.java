package dgm.elasticsearch;

import com.google.common.io.Files;
import com.google.inject.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Configure ES node that joins the cluster, no data etc.
 */
public class NodeES extends AbstractModule
{
    protected final String cluster;
    protected final String host;
    protected final int port;

    static Logger log = LoggerFactory.getLogger(TransportES.class);

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
    final Client provideElasticInterface()
    {
        // create temporary directory and use this as ES datadir
        final File dataDir = Files.createTempDir();

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

        return NodeBuilder.nodeBuilder().settings(settings).node().client();
    }
}