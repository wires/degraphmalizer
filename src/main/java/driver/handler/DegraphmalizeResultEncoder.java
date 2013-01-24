package driver.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import degraphmalizr.jobs.*;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 *
 */
public class DegraphmalizeResultEncoder extends OneToOneEncoder
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Class[] acceptedMessages = new Class[]{DegraphmalizeAction.class, DegraphmalizeResult.class, RecomputeAction.class, RecomputeResult.class};

    @Override
    protected final Object encode(ChannelHandlerContext ctx, Channel channel, Object o)
    {
        final Class oc = o.getClass();
        for(Class c : acceptedMessages)
                if(c.isAssignableFrom(oc))
                {
//                    final JsonNode n = objectMapper.valueToTree(o);
//                    final HttpResponse r = new DefaultHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
//                    r.setContent(ChannelBuffers.copiedBuffer(n.toString(), Charsets.UTF_8));
//                    return r;

                    final ObjectNode n = objectMapper.createObjectNode();
                    n.put("type", c.getSimpleName());

                    final HttpResponse r = new DefaultHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
                    r.setContent(ChannelBuffers.copiedBuffer(n.toString(), Charsets.UTF_8));

                    return r;
                }

        // otherwise pass it on
        return o;
    }
}