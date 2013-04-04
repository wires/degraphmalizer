package dgm.modules.fsmon;

import dgm.configuration.Configuration;
import dgm.configuration.ConfigurationMonitor;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

class LoggingConfigurationMonitor implements ConfigurationMonitor
{
    @InjectLogger
    Logger log;

    @Override
    public void configurationChanged(String change)
    {
        log.info("Configuration change for index {}", change);
    }
}


class LoggingFilesystemMonitor implements FilesystemMonitor
{
    @InjectLogger
    Logger log;

    @Override
    public void directoryChanged(String directory)
    {
        log.info("Filesystem change detected for directory {}", directory);
    }
}

/**
 * Monitor the filesystem for changes and then provide a new configuration
 */
public class DynamicConfiguration extends AbstractConfigurationModule
{
    public DynamicConfiguration(String scriptFolder, String... libraries)
    {
        super(scriptFolder, libraries);
    }

    @Override
    protected void configureModule()
    {
        // setup poller service
        bindService(PollingFilesystemMonitorService.class);

        // send filesystem notifications to the configuration reloader
        multiBind(FilesystemMonitor.class).to(LoggingFilesystemMonitor.class);
        multiBind(FilesystemMonitor.class).to(ConfigurationReloader.class);

        // we need at least one binding to configuration monitor or guice complains
        multiBind(ConfigurationMonitor.class).to(LoggingConfigurationMonitor.class);

        bind(Configuration.class).toProvider(ConfigurationReloader.class);
    }
}