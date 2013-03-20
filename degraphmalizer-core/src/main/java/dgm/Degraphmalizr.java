package dgm;

import dgm.degraphmalizr.degraphmalize.*;
import dgm.exceptions.DegraphmalizerException;

public interface Degraphmalizr
{
    DegraphmalizeAction degraphmalize(DegraphmalizeActionType actionType, DegraphmalizeActionScope actionScope, ID id, DegraphmalizeStatus callback) throws DegraphmalizerException;
}