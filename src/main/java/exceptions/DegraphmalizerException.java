package exceptions;

/**
 * Super class of all degraphmalizer exceptions
 */
public class DegraphmalizerException extends RuntimeException
{
    protected DegraphmalizerException(String msg, Exception cause)
    {
        super(msg, cause);
    }

    protected DegraphmalizerException(String msg)
    {
        super(msg);
    }
}