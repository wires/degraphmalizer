package degraphmalizr;

import degraphmalizr.jobs.DegraphmalizeAction;
import degraphmalizr.jobs.DegraphmalizeStatus;
import exceptions.DegraphmalizerException;

import java.util.List;

public interface Degraphmalizr
{
    List<DegraphmalizeAction> degraphmalize(ID id, DegraphmalizeStatus callback) throws DegraphmalizerException;
}
