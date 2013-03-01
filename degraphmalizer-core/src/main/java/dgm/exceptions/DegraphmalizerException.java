package dgm.exceptions;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * Super class of all degraphmalizer exceptions
 */
public class DegraphmalizerException extends RuntimeException
{
    private final Severity severity;

    public enum Severity { INFO, WARN, ERROR }

    protected DegraphmalizerException(String msg, Throwable cause)
    {
        super(msg, cause);
        this.severity = Severity.ERROR;
    }

    protected DegraphmalizerException(String msg, Severity severity)
    {
        super(msg);
        this.severity = severity;
    }

    protected DegraphmalizerException(String msg)
    {
        super(msg);
        this.severity = Severity.ERROR;
    }

    public Severity severity()
    {
        return severity;
    }

    public HttpResponseStatus httpStatusCode()
    {
        switch (severity())
        {
            case INFO:
            case WARN:
                return HttpResponseStatus.OK;

            case ERROR:
            default:
                return HttpResponseStatus.BAD_REQUEST;
        }
    }
}