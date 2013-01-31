package modules;

import com.google.inject.*;
import configuration.*;
import configuration.javascript.JavascriptConfiguration;
import configuration.javascript.PollingConfigurationMonitor;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CachedProvider<T> implements Provider<T>
{
    final Provider<T> sourceProvider;

    // current cached is cached here
    T cached = null;

    public CachedProvider(Provider<T> sourceProvider)
    {
        this.sourceProvider = sourceProvider;
        this.cached = sourceProvider.get();
    }

    public T get()
    {
        // (for thread safety)
        final T c = cached;

        // still up to date
        if (c != null)
            return c;

        // create new cached
        final T d = sourceProvider.get();

        // return old config if loading failed
        //todo makes no sense: c == null
        if (d == null)
            return c;

        cached = d;
        return d;
    }

    public void invalidate()
    {
        cached = null;
    }
}

/**
 * Non-reloading javascript cached
 */
public class ReloadingJSConfModule extends AbstractModule implements ConfigurationMonitor
{
    private static final Logger log = LoggerFactory.getLogger(ReloadingJSConfModule.class);

	final String scriptFolder;
    final PollingConfigurationMonitor poller;
    final CachedProvider<Configuration> cachedProvider;

    public ReloadingJSConfModule(final String scriptFolder) throws IOException
    {
        final Provider<Configuration> confLoader = new Provider<Configuration>() {
            @Override public Configuration get() {
                try
                {
                    return new JavascriptConfiguration(new File(scriptFolder));
                }
                catch (Exception e)
                {
                    log.info("Failed to load configuration, {}", e.getMessage());
                    return null;
                }
            }
        };

        this.cachedProvider = new CachedProvider<Configuration> (confLoader);
        this.scriptFolder = scriptFolder;
        this.poller = new PollingConfigurationMonitor(scriptFolder, 200, this);

        // start the poller (does nothing if it is already running)
        poller.start();
    }
	
    @Provides
    public final Configuration provideConfiguration() throws IOException
	{
        return cachedProvider.get();
    }

    @Override
    public final void configurationChanged(String index)
    {
        log.info("Configuration change detected for target-index {}", index);
        cachedProvider.invalidate();

        // print configuration if debugging is enabled
        if(!log.isDebugEnabled())
            return;

        // get new config
        final Configuration cfg = cachedProvider.get();
        for(final IndexConfig i : cfg.indices().values())
            for(final TypeConfig t : i.types().values())
                log.debug("Found target configuration /{}/{} --> /{}/{}",
                        new Object[]{t.sourceIndex(), t.sourceType(), i.name(), t.name()});
    }

    @Override
    protected void configure()
    {}
}
