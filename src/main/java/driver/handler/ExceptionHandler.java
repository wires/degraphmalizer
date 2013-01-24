package driver.handler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import exceptions.DegraphmalizerException;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;

import java.io.StringWriter;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;

public class ExceptionHandler extends SimpleChannelHandler
{
    // TODO use annotated POJO messages and inject the objectmapper
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected final void renderException(ObjectNode parent, Throwable t)
    {
        final ArrayNode ss = objectMapper.createArrayNode();
        for(StackTraceElement elt : t.getStackTrace())
            ss.add(elt.toString());

        parent.put("message", t.getMessage());
        parent.put("exception", t.getClass().getSimpleName());
        parent.put("stacktrace", ss);
    }

    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
        final Channel c = ctx.getChannel();
        final Throwable t = e.getCause();

        // TODO wrap in "data" element according to JSEND ?
        // construct JSEND style JSON response http://labs.omniti.com/labs/jsend

        final ObjectNode root = objectMapper.createObjectNode();
        root.put("status", "error");
        renderException(root, t);

        // add optional cause
        final Throwable cause = t.getCause();
        if(cause != null)
        {
            final ObjectNode cnode = objectMapper.createObjectNode();
            root.put("cause", cnode);
            renderException(cnode, cause);
        }

        final StringWriter sw = new StringWriter();
        final JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
        objectMapper.writeTree(gen, root);

        // If the exception is known, we report "BAD_REQUEST" (user problem)
        final boolean knownException = DegraphmalizerException.class.isAssignableFrom(t.getClass());
        final HttpResponseStatus status = knownException ? BAD_REQUEST : INTERNAL_SERVER_ERROR;

        final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_0, status);
        response.setContent(ChannelBuffers.copiedBuffer(sw.toString(), Charsets.UTF_8));

        if(c.isOpen() && c.isWritable())
            c.write(response).addListener(ChannelFutureListener.CLOSE);
    }
}