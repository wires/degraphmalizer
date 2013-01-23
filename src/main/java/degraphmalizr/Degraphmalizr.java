package degraphmalizr;

import degraphmalizr.jobs.DegraphmalizeAction;
import degraphmalizr.jobs.DegraphmalizeStatus;
import exceptions.DegraphmalizerException;

public interface Degraphmalizr
{
    DegraphmalizeAction degraphmalize(ID id, DegraphmalizeStatus callback) throws DegraphmalizerException;
}
