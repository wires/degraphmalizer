package exceptions;

/**
 * Super class of all degraphmalizer exceptions
 */
public class DegraphmalizerException extends Exception
{
    public DegraphmalizerException(String msg, Exception cause)
    {
        super(msg, cause);
    }

    public DegraphmalizerException(String msg)
    {
        super(msg);
    }
}