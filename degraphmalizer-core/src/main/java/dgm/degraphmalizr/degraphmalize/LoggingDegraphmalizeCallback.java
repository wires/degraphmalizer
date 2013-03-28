package dgm.degraphmalizr.degraphmalize;

import dgm.exceptions.DegraphmalizerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ernst Bunders
 */
public class LoggingDegraphmalizeCallback implements DegraphmalizeCallback
{
    private static final Logger logger = LoggerFactory.getLogger(LoggingDegraphmalizeCallback.class);

    @Override
    public void started(DegraphmalizeRequest request)
    {
        logger.debug("Started: {}", request);
    }

    @Override
    public void complete(DegraphmalizeResult result)
    {
        logger.debug("Complete: {}", result);
    }

    @Override
    public void failed(DegraphmalizerException exception)
    {
        logger.debug("Failed: {}", exception);
    }
}
