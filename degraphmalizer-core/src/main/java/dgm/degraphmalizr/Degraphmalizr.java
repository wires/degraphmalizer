package dgm.degraphmalizr;

import dgm.degraphmalizr.degraphmalize.*;
import dgm.exceptions.DegraphmalizerException;

public interface Degraphmalizr
{
    DegraphmalizeAction degraphmalize(DegraphmalizeActionType actionType, ID id, DegraphmalizeStatus callback) throws DegraphmalizerException;

}
