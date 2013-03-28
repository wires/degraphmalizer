package dgm.degraphmalizr.degraphmalize;

import dgm.ID;
import dgm.degraphmalizr.recompute.RecomputeResult;

import java.util.List;
import java.util.concurrent.Future;

public class DegraphmalizeResult
{
    protected final ID root;
    protected final List<Future<RecomputeResult>> results;

    public DegraphmalizeResult(ID root, List<Future<RecomputeResult>> results)
    {
        this.root = root;
        this.results = results;
    }

    public List<Future<RecomputeResult>> results()
    {
        return results;
    }

    public ID root()
    {
        return root;
    }
}