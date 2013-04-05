package dgm.modules.elasticsearch.nodes;

import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.File;

/**
 * Configure local ES node with state in temporary directory.
 * This means that you start up with a clean, fresh embedded ES every time.
 */
public class EphemeralES extends AbstractModule
{
    @Override
    protected void configure()
    {}

    @Provides @Singleton
    final Node provideNode()
    {
        // create temporary directory and use this as ES datadir
        final File tempDir = Files.createTempDir();

        // otherwise run as much as possible in memory
        final Settings.Builder settings = ImmutableSettings.settingsBuilder()
                .put("path.data", pathConcat(tempDir, "data"))
                .put("path.logs", pathConcat(tempDir, "logs"))
                .put("path.work", pathConcat(tempDir, "work"))
                .put("node.http.enabled", false)
                .put("node.local", true)
                .put("node.data", true)
                .put("gateway.type", "none")
                .put("index.store.type", "memory")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0);

        return NodeBuilder.nodeBuilder().settings(settings).build();
    }

    private String pathConcat(File tempDir, String subdir)
    {
        return tempDir.getAbsolutePath() + File.separator + subdir;
    }
}
