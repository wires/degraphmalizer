package degraphmalizr;

import degraphmalizr.jobs.DegraphmalizeAction;
import degraphmalizr.jobs.DegraphmalizeActionType;
import degraphmalizr.jobs.DegraphmalizeStatus;
import exceptions.DegraphmalizerException;

import java.util.List;

public interface Degraphmalizr
{
    List<DegraphmalizeAction> degraphmalize(DegraphmalizeActionType actionType, ID id, DegraphmalizeStatus callback) throws DegraphmalizerException;

}
