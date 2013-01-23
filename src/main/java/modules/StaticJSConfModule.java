package modules;

import com.google.inject.*;
import configuration.Configuration;
import configuration.javascript.JavascriptConfiguration;

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
    Configuration provideConfiguration() throws IOException
	{
		return new JavascriptConfiguration(new File(scriptFolder));
	}
}
