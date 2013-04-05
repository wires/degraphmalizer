package dgm.modules.elasticsearch;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dgm.Service;
import dgm.modules.ServiceModule;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

class ElasticSearchService implements Service
{
    final Node node;
    final Client client;

    @InjectLogger
    Logger log;

    @Inject
    ElasticSearchService(Node node, Client client)
    {
        this.node = node;
        this.client = client;
    }

    @Override
    public void start()
    {
        node.start();

        // get some information on the cluster (and thus check if we can talk to it)
        final ClusterStateResponse cr = node.client().admin().cluster().prepareState().execute().actionGet();
        log.info("Connected to cluster with {} nodes", cr.state().nodes().getSize());
    }

    @Override
    public void stop()
    {
        client.close();
        node.close();
    }
}

public class CommonElasticSearchModule extends ServiceModule
{
    @Override
    protected void configure()
    {
        bindService(ElasticSearchService.class);
    }

    // if we have a node, we can always just get a client from it
    @Provides @Singleton
    final Client provideClient(Node node)
    {
        return node.client();
    }
}