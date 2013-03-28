/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.degraphmalizr.degraphmalize;

import dgm.ID;
import dgm.configuration.TypeConfig;

/**
 * User: rico
 * Date: 27/03/2013
 */
public class DegraphmalizeRequest
{
    public final DegraphmalizeRequestType requestType;
    public final DegraphmalizeRequestScope requestScope;
    public final ID id;
    public final Iterable<TypeConfig> configs;


    public DegraphmalizeRequest(DegraphmalizeRequestType requestType, DegraphmalizeRequestScope requestScope, ID id, Iterable<TypeConfig> configs)
    {
        this.requestType = requestType;
        this.requestScope = requestScope;
        this.id = id;
        this.configs = configs;
    }

    public DegraphmalizeRequestType type()
    {
        return requestType;
    }

    public DegraphmalizeRequestScope scope()
    {
        return requestScope;
    }

    public ID id()
    {
        return id;
    }

    public Iterable<TypeConfig> configs()
    {
        return configs;
    }
}
