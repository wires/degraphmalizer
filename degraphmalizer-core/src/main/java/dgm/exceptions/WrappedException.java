package dgm.exceptions;

/**
 *
 */
public class WrappedException extends DegraphmalizerException
{
    public WrappedException(Exception cause)
    {
        super("Unknown exception occurred", cause);
    }

    public String toString() {
        return getCause().getClass()+": "+getCause().getMessage();
    }
}