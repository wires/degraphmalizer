package degraphmalizr.jobs;

public interface DegraphmalizeStatus
{
    void recomputeStarted(RecomputeAction action);
    void recomputeComplete(RecomputeResult result);

    /**
     * Called when the job is finished successfully
     *
     * @param result
     */
    void complete(DegraphmalizeResult result);

    /**
     * Called when the action has failed because of an exception.
     *
     * {@link degraphmalizr.jobs.DegraphmalizeResult#succes()} will be false, and the exception causing the problem
     * can be retrieved from {@link degraphmalizr.jobs.DegraphmalizeResult#exception()}.
     *
     * @param result
     */
    void exception(DegraphmalizeResult result);
}
