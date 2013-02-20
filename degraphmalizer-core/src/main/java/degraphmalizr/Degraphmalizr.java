package degraphmalizr;

import degraphmalizr.degraphmalize.*;
import exceptions.DegraphmalizerException;

public interface Degraphmalizr
{
    DegraphmalizeAction degraphmalize(DegraphmalizeActionType actionType, ID id, DegraphmalizeStatus callback) throws DegraphmalizerException;

}
