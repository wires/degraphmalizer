package dgm.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import dgm.configuration.Configuration;

import java.io.IOException;

/**
 * Non-reloading javascript configuration
 */
public class StaticJSConfModule extends AbstractModule
{
	final String scriptFolder;
    final String[] libraries;
	
	public StaticJSConfModule(String scriptFolder, String... libraries)
	{
		this.scriptFolder = scriptFolder;
        this.libraries = libraries;
	}
	
	@Override
    protected void configure()
    {}	
	
	@Provides
	@Singleton
    @Inject
    final Configuration provideConfiguration(ObjectMapper om) throws IOException
	{
		return ReloadingJSConfModule.createConfiguration(om, scriptFolder, libraries);
	}
}
