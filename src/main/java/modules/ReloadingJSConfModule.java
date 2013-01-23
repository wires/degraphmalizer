package modules;

import com.google.inject.*;
import configuration.Configuration;
import configuration.ConfigurationMonitor;
import configuration.javascript.JavascriptConfiguration;
import configuration.javascript.PollingConfigurationMonitor;

import java.io.File;
import java.io.IOException;

/**
 * Non-reloading javascript configuration
 */
public class ReloadingJSConfModule extends AbstractModule implements ConfigurationMonitor
{
	final String scriptFolder;
    final PollingConfigurationMonitor poller;

    // current configuration is cached here
    JavascriptConfiguration configuration = null;

    public ReloadingJSConfModule(String scriptFolder)
	{
        this.scriptFolder = scriptFolder;
        this.poller = new PollingConfigurationMonitor(scriptFolder, 200, this);
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
        final JavascriptConfiguration d = new JavascriptConfiguration(new File(scriptFolder));

        configuration = d;
        return d;
	}

    @Override
    public void configurationChanged(String index)
    {
        // when the configurating changes, mark it for (lazy) reloading
        configuration = null;

        // TODO fix logging
        System.err.println("Configuration change detected for index '" + index + "'");
    }
}
