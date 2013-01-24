package elasticsearch;

import com.google.inject.*;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;


public class TransportES extends AbstractModule
{
    protected final String cluster;
    protected final String host;
    protected final int port;

    static Logger log = LoggerFactory.getLogger(TransportES.class);

    public TransportES(String cluster, String host, int port)
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
    final Client providesClient()
    {
        final Settings s = settingsBuilder().put("cluster.name", cluster).build();

        final TransportClient client = new TransportClient(s);
        client.addTransportAddress(new InetSocketTransportAddress(host, port));

        // get some information on the cluster (and thus check if we can talk to it)
        final ClusterStateResponse cr = client.admin().cluster().prepareState().execute().actionGet();
        log.info("Connected to cluster with {} nodes", cr.state().nodes().getSize());

        return client;
    }
}
