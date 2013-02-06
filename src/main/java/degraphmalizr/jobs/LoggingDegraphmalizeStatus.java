package degraphmalizr.jobs;

import org.slf4j.Logger;

/**
 * @author Ernst Bunders
 */
public class LoggingDegraphmalizeStatus implements DegraphmalizeStatus
{
    private final Logger logger;

    public LoggingDegraphmalizeStatus(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void recomputeStarted(RecomputeAction action)
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
        logger.debug("Degraphmalize complete: {}", result);
    }

    @Override
    public void exception(DegraphmalizeResult result)
    {
        logger.debug("Degraphmalize exception: {}", result);
    }
}
