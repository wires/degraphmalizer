package degraphmalizr;

import degraphmalizr.degraphmalize.DegraphmalizeAction;
import degraphmalizr.degraphmalize.DegraphmalizeActionType;
import degraphmalizr.degraphmalize.DegraphmalizeStatus;
import exceptions.DegraphmalizerException;

import java.util.List;

public interface Degraphmalizr
{
    List<DegraphmalizeAction> degraphmalize(DegraphmalizeActionType actionType, ID id, DegraphmalizeStatus callback) throws DegraphmalizerException;

}
