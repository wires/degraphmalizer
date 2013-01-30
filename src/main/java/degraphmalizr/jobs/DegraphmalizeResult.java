package degraphmalizr.jobs;

import exceptions.DegraphmalizerException;

public class DegraphmalizeResult
{
    private final DegraphmalizeOutcome outcome;
    private final DegraphmalizeAction action;
    private final DegraphmalizerException exception;

    public DegraphmalizeResult(DegraphmalizeOutcome outcome, DegraphmalizeAction action)
    {
        this(outcome, action, null);
    }

    public DegraphmalizeResult(DegraphmalizeOutcome outcome, DegraphmalizeAction action, DegraphmalizerException ex)
    {
        this.outcome = outcome;
        this.action = action;
        this.exception = ex;
    }

    public DegraphmalizeOutcome outcome()
    {
        return outcome;
    }

    public DegraphmalizeAction action()
    {
        return action;
    }

    public Exception exception()
    {
        return exception;
    }

    public static DegraphmalizeResult success(DegraphmalizeAction action) {
        return new DegraphmalizeResult(DegraphmalizeOutcome.SUCCESS, action);
    }

    public static DegraphmalizeResult failure(DegraphmalizeAction action, DegraphmalizerException e) {
        return new DegraphmalizeResult(DegraphmalizeOutcome.FAILURE, action, e);
    }
}