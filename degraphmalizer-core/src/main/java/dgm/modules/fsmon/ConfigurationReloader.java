package dgm.modules.fsmon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import dgm.configuration.*;
import dgm.exceptions.ConfigurationException;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


@Singleton
class ConfigurationReloader implements FilesystemMonitor, Provider<Configuration>
{
    @InjectLogger
    Logger log;

    final String scriptFolder;
    final List<File> libraries;
    final CachedProvider<Configuration> cachedProvider;

    final ConfigurationMonitor configurationMonitor;

    final ObjectMapper om = new ObjectMapper();

    @Inject
    public ConfigurationReloader(Set<ConfigurationMonitor> configurationMonitors,
                                 final @Named("scriptFolder") String scriptFolder,
                                 final @Named("libraryFiles") List<File> libraries) throws IOException
    {
        this.scriptFolder = scriptFolder;
        this.libraries = libraries;

        configurationMonitor = new CompositeConfigurationMonitor(configurationMonitors);

        /**
         * When loading the configuration this will throw an exception with an invalid configuration, but only on startup.
         * When already running with a configuration this will log an error message trying to load an invalid configuration, but
         * will continue to run with the previous valid configuration.
         */
        final Provider<Configuration> confLoader = new Provider<Configuration>()
        {
            private AtomicReference<Configuration> configuration =
                    new AtomicReference<Configuration>(AbstractConfigurationModule.createConfiguration(om, scriptFolder, libraries));

            @Override public Configuration get() {
                try
                {
                    final Configuration d = configuration.getAndSet(null);
                    if (d == null)
                        return AbstractConfigurationModule.createConfiguration(om, scriptFolder, libraries);

                    return d;
                }
                catch (ConfigurationException ce)
                {
                    log.info("Failed to load configuration, {}", ce.getMessage());
                    return null;
                }
                catch (Exception e)
                {
                    log.info("Unknown Exception while loading configuration, {}", e);
                    return null;
                }
            }
        };

        this.cachedProvider = new CachedProvider<Configuration> (confLoader);
    }

    @Override
    public void directoryChanged(String directory)
    {
        log.info("Filesystem change detected for directory (target-index) {}", directory);

        // try to reload the configuration
        if(!cachedProvider.invalidate())
            log.info("Failed to reload configuration");

        // print configuration if debugging is enabled
        if(log.isDebugEnabled())
        {
            // get new config
            final Configuration cfg = cachedProvider.get();
            for(final IndexConfig i : cfg.indices().values())
                for(final TypeConfig t : i.types().values())
                    log.debug("Found target configuration /{}/{} --> /{}/{}",
                            new Object[]{t.sourceIndex(), t.sourceType(), i.name(), t.name()});
        }

        // notify all configuration listeners
        configurationMonitor.configurationChanged(directory);
    }

    @Override
    public Configuration get()
    {
        return cachedProvider.get();

    }
}
