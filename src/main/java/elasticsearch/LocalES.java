package elasticsearch;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import static org.elasticsearch.node.NodeBuilder.*;

/**
 * Configure local ES
 */
public class LocalES extends AbstractModule
{
    @Override
    protected void configure()
    {}

    @Provides
    @Singleton
    Client provideElasticInterface()
    {
        final Node node = nodeBuilder().local(true).node();
        return node.client();
    }
}
