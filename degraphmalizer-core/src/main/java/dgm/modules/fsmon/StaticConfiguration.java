package dgm.modules.fsmon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import dgm.configuration.Configuration;

import java.io.IOException;

/**
 * Non-reloading javascript configuration
 */
public class StaticConfiguration extends AbstractConfigurationModule
{
    public StaticConfiguration(String scriptFolder, String... libraries)
    {
        super(scriptFolder, libraries);
    }

    @Provides @Singleton @Inject
    final Configuration provideConfiguration(ObjectMapper om) throws IOException
	{
		return createConfiguration(om, scriptFolder, libraries);
	}

    @Override
    protected void configureModule()
    {}
}
