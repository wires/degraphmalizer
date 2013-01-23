package modules;

import com.google.inject.*;
import configuration.Configuration;
import configuration.ConfigurationMonitor;
import configuration.javascript.JavascriptConfiguration;
import configuration.javascript.PollingConfigurationMonitor;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non-reloading javascript configuration
 */
public class ReloadingJSConfModule extends AbstractModule implements ConfigurationMonitor
{
    private final static Logger log = LoggerFactory.getLogger(ReloadingJSConfModule.class);

	final String scriptFolder;
    final PollingConfigurationMonitor poller;

    // current configuration is cached here
    JavascriptConfiguration configuration = null;

    public ReloadingJSConfModule(String scriptFolder) throws IOException
    {
        this.scriptFolder = scriptFolder;
        this.poller = new PollingConfigurationMonitor(scriptFolder, 200, this);

        if(loadConf() == null)
            throw new RuntimeException("Failed loading configuration!");
    }
	
	@Override
    protected void configure()
    {}
	
	@Provides
    Configuration provideConfiguration() throws IOException
	{
        final Configuration c = configuration;

        // still up to date
        if (c != null)
            return c;

        // start the poller (does nothing if it is already running)
        poller.start();

        // create new configuration
        final JavascriptConfiguration d = loadConf();

        configuration = d;
        return d;
	}

    @Override
    public void configurationChanged(String index)
    {
        // when the configurating changes, mark it for (lazy) reloading
        configuration = null;

        log.info("Configuration change detected for index {}", index);
    }

    private JavascriptConfiguration loadConf() throws IOException
    {
        return new JavascriptConfiguration(new File(scriptFolder));
    }
}
