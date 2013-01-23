package modules;

import com.google.inject.*;
import graphs.LoggingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogconfModule extends AbstractModule
{
    @Provides
    @Singleton
    Logger provideLogger()
    {
        // TODO this is a hack until Sli4j works
        return LoggerFactory.getLogger("Main");
    }

	@Provides
	@Singleton
	LoggingProvider provideLoggingProvider()
	{
		return new LoggingProvider()
			{
				@Override
				public Logger getNamedLogger(String name)
				{
					return LoggerFactory.getLogger(name);
				}
			};
	}

    @Override
    protected void configure()
    {}
}