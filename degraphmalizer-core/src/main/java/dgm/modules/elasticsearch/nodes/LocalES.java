package dgm.modules.elasticsearch.nodes;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

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
    final Node provideElasticInterface()
    {
        final Settings.Builder settings = ImmutableSettings.settingsBuilder()
                .put("node.http.enabled", true)
                .put("node.local", true)
                .put("node.data", true)
                .put("index.store.type", "memory")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0);

        return NodeBuilder.nodeBuilder().settings(settings).build();
    }
}
