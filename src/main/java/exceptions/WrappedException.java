package exceptions;

/**
 *
 */
public class WrappedException extends DegraphmalizerException
{
    public WrappedException(Exception cause)
    {
        super("Unknown exception occurred", cause);
    }
}