package degraphmalizr;


import degraphmalizr.jobs.RecomputeAction;
import degraphmalizr.jobs.RecomputeResult;

interface RecomputeCallback
{
    void recomputeSuccess(RecomputeAction parent, RecomputeResult.Success success);
}

interface Recompute
{
    RecomputeResult recompute(RecomputeAction action, RecomputeCallback callback);
}

public class Recomputer
{
}
