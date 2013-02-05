package degraphmalizr.jobs;

import degraphmalizr.recompute.RecomputeRequest;
import degraphmalizr.recompute.RecomputeResult;

public interface DegraphmalizeStatus
{
    void recomputeStarted(RecomputeRequest action);
    void recomputeComplete(RecomputeResult result);

    /**
     * Called when the job is finished successfully
     */
    void complete(DegraphmalizeResult result);

    /**
     * Called when the action has failed because of an exception.
     *
     * {@link degraphmalizr.jobs.DegraphmalizeResult#outcome()} will be false, and the exception causing the problem
     * can be retrieved from {@link degraphmalizr.jobs.DegraphmalizeResult#exception()}.
     */
    void exception(DegraphmalizeResult result);
}
