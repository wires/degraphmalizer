/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.degraphmalizr.degraphmalize;

import dgm.exceptions.DegraphmalizerException;

/**
 * User: rico
 * Date: 27/03/2013
 */
public interface DegraphmalizeCallback {
    void started(DegraphmalizeRequest request);
    void complete(DegraphmalizeResult result);
    void failed(DegraphmalizerException exception);
}
