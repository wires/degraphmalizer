package dgm.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import dgm.configuration.Configuration;
import dgm.configuration.javascript.JavascriptConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Non-reloading javascript configuration
 */
public class StaticJSConfModule extends AbstractModule
{
	final String scriptFolder;
	
	public StaticJSConfModule(String scriptFolder)
	{
		this.scriptFolder = scriptFolder;
	}
	
	@Override
    protected void configure()
    {}	
	
	@Provides
	@Singleton
    @Inject
    final Configuration provideConfiguration(ObjectMapper om) throws IOException
	{
		return new JavascriptConfiguration(om, new File(scriptFolder));
	}
}
