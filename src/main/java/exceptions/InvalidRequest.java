package exceptions;

public class InvalidRequest extends RuntimeException
{
    public InvalidRequest(String msg)
    {
        super(msg);
    }
}