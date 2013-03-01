package dgm.driver.handler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import dgm.exceptions.DegraphmalizerException;
import dgm.exceptions.WrappedException;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

public class ExceptionHandler extends SimpleChannelHandler
{
    // TODO use annotated POJO messages and inject the objectmapper
    private static final ObjectMapper objectMapper = new ObjectMapper();

    Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
        final Channel c = ctx.getChannel();
        final DegraphmalizerException ex = wrapException(e.getCause());
        final String json = renderExceptionResponse(objectMapper, ex);

        final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_0, ex.httpStatusCode());
        response.setContent(ChannelBuffers.copiedBuffer(json, Charsets.UTF_8));

        logException(ex);

        if(c.isOpen() && c.isWritable())
            c.write(response).addListener(ChannelFutureListener.CLOSE);
    }

    // log according to severity
    void logException(DegraphmalizerException ex)
    {
        switch (ex.severity())
        {
            case INFO:
                log.info(ex.getMessage(), ex);
                return;
            case WARN:
                log.warn(ex.getMessage(), ex);
                return;
            case ERROR:
            default:
                log.error(ex.getMessage(), ex);
                return;
        }
    }

    // wrap the exception if needed
    private DegraphmalizerException wrapException(Throwable t)
    {
        if(DegraphmalizerException.class.isAssignableFrom(t.getClass()))
            return (DegraphmalizerException)t;

        return new WrappedException(t);
    }

    public static String renderExceptionResponse(ObjectMapper om, DegraphmalizerException ex) throws IOException
    {
        // TODO wrap in "data" element according to JSEND ?
        // construct JSEND style JSON response http://labs.omniti.com/labs/jsend

        final ObjectNode root = renderException(om, ex);
        root.put("status", "error");
        root.put("severity", ex.severity().name().toLowerCase());

        // add optional cause
        final Throwable cause = ex.getCause();
        if(cause != null)
            root.put("cause", renderException(om, cause));

        final StringWriter sw = new StringWriter();
        final JsonGenerator gen = new JsonFactory().createJsonGenerator(sw).useDefaultPrettyPrinter();
        om.writeTree(gen, root);

        return sw.toString();
    }

    public static ObjectNode renderException(ObjectMapper om, Throwable t)
    {
        final ArrayNode ss = om.createArrayNode();
        for(StackTraceElement elt : t.getStackTrace())
            ss.add(elt.toString());

        final ObjectNode ex = om.createObjectNode();
        ex.put("message", t.getMessage());
        ex.put("class", t.getClass().getSimpleName());
        ex.put("stacktrace", ss);

        return ex;
    }
}