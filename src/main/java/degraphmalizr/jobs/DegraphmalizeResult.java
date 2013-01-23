package degraphmalizr.jobs;

public interface DegraphmalizeResult
{
    boolean succes();
    DegraphmalizeAction action();
    Exception exception();
}
