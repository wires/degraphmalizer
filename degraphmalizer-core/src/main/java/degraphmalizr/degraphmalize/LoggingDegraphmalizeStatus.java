package degraphmalizr.degraphmalize;

import degraphmalizr.recompute.RecomputeRequest;
import degraphmalizr.recompute.RecomputeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ernst Bunders
 */
public class LoggingDegraphmalizeStatus implements DegraphmalizeStatus
{
    private static final Logger logger = LoggerFactory.getLogger(LoggingDegraphmalizeStatus.class);

    @Override
    public void recomputeStarted(RecomputeRequest action)
    {
        logger.debug("Recompute started: {}", action);
    }

    @Override
    public void recomputeComplete(RecomputeResult result)
    {
        logger.debug("Recompute complete: {}", result);
    }

    @Override
    public void complete(DegraphmalizeResult result)
    {
        logger.debug("Complete: {}", result);
    }

    @Override
    public void exception(DegraphmalizeResult result)
    {
        logger.debug("Degraphmalize exception: {}", result);
    }
}
