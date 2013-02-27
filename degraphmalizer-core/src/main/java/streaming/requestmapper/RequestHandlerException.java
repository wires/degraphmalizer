package streaming.requestmapper;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Ernst Bunders
 */
public class RequestHandlerException extends Exception
{
    protected final int code;

    public RequestHandlerException(String msg, HttpResponseStatus s)
    {
        super(msg);
        this.code = s.getCode();
    }

    public final HttpResponseStatus getStatus()
    {
        return HttpResponseStatus.valueOf(code);
    }
}