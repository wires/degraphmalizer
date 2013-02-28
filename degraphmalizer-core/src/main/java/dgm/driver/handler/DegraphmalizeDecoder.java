package dgm.driver.handler;

import dgm.exceptions.InvalidRequest;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import dgm.ID;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeActionType;
import dgm.degraphmalizr.degraphmalize.JobRequest;
import dgm.exceptions.DegraphmalizerException;

/**
 * Transform a HttpRequest into
 */
public class DegraphmalizeDecoder extends OneToOneDecoder
{
    @Override
    protected final Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, Object o) throws DegraphmalizerException
    {
        final HttpRequest request = (HttpRequest)o;

        // split url /TYPE/ID/ or fail
        final String[] components = request.getUri().substring(1).split("/");

        if (components.length != 4)
            throw new InvalidRequest("URL must be of the form '/{index}/{type}/{id}/{version}'");

        final DegraphmalizeActionType actionType = actionTypeFor(request);

        final String index = components[0];
        final String type = components[1];
        final String id = components[2];
        final long v = Long.parseLong(components[3]);

        return new JobRequest(actionType, new ID(index, type, id, v));
    }

    // HTTP.method ? DELETE => anti-degraphmalize it
    private static DegraphmalizeActionType actionTypeFor(HttpRequest req)
    {
        if (HttpMethod.DELETE.equals(req.getMethod()))
            return DegraphmalizeActionType.DELETE;

        return DegraphmalizeActionType.UPDATE;
    }
}