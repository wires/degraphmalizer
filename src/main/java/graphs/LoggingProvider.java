package graphs;

import org.slf4j.Logger;

public interface LoggingProvider
{	
	Logger getNamedLogger(String name);
}
