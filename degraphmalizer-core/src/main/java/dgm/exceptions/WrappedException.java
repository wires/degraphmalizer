package dgm.exceptions;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

/**
 *
 */
public class WrappedException extends DegraphmalizerException
{
    public WrappedException(Throwable cause)
    {
        super("Unknown exception occurred", cause);
    }

    public String toString()
    {
        return getCause().getClass() + ": " + getCause().getMessage();
    }

    @Override
    public Severity severity()
    {
        return Severity.ERROR;
    }

    @Override
    public HttpResponseStatus httpStatusCode()
    {
        return INTERNAL_SERVER_ERROR;
    }
}