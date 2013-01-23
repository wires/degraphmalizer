package elasticsearch;

import com.google.inject.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;


public class TransportES extends AbstractModule
{
    protected final String cluster;
    protected final String host;
    protected final int port;

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
    Client providesClient()
    {
        final Settings s = settingsBuilder().put("cluster.name", cluster).build();

        final TransportClient client = new TransportClient(s);
        client.addTransportAddress(new InetSocketTransportAddress(host, port));

        return client;
    }
}
