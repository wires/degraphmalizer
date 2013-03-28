package dgm;

import dgm.degraphmalizr.degraphmalize.DegraphmalizeRequestScope;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeRequestType;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeCallback;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeResult;
import dgm.exceptions.DegraphmalizerException;

import java.util.concurrent.Future;

public interface Degraphmalizr
{
    Future<DegraphmalizeResult> degraphmalize(DegraphmalizeRequestType requestType, DegraphmalizeRequestScope requestScope, ID id, DegraphmalizeCallback callback) throws DegraphmalizerException;
}