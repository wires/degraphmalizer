package dgm.degraphmalizr.degraphmalize;

import dgm.ID;

public class JobRequest
{
    private final DegraphmalizeRequestType requestType;
    private final DegraphmalizeRequestScope requestScope;
    private final ID id;

    public JobRequest(DegraphmalizeRequestType requestType, DegraphmalizeRequestScope requestScope, ID id)
    {
        this.requestType = requestType;
        this.requestScope = requestScope;
        this.id = id;
    }

    public DegraphmalizeRequestType actionType()
    {
        return requestType;
    }

    public DegraphmalizeRequestScope actionScope()
    {
        return requestScope;
    }

    public ID id()
    {
        return id;
    }
}
