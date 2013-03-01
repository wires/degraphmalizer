package dgm.exceptions;

import dgm.ID;

public class NoConfiguration extends DegraphmalizerException
{
    public NoConfiguration(ID id)
    {
        super("No matching configuration for index=" + id.index() + ", type=" + id.type(), Severity.INFO);
    }
}