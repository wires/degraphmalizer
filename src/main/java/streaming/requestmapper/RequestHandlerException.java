package streaming.requestmapper;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Ernst Bunders
 */
public class RequestHandlerException extends Exception {
    private final HttpResponseStatus status;

    public RequestHandlerException(String s, HttpResponseStatus status) {
        super(s);
        this.status = status;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }
}
