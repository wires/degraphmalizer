package dgm.driver.handler;

import dgm.degraphmalizr.degraphmalize.DegraphmalizeActionScope;
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
        final DegraphmalizeActionType actionType = actionTypeFor(request);

        // split url /TYPE/ID/ or fail
        final String[] components = request.getUri().substring(1).split("/");

        switch (actionType)
        {
            case DELETE:
                if (components.length<1 || components.length>4)
                    throw new InvalidRequest("URL must be of the form '/{index}/{type}/{id}/{version}'");
                break;
            case UPDATE:
                if (components.length != 4)
                    throw new InvalidRequest("URL must be of the form '/{index}/{type}/{id}/{version}'");
                break;
            default:
                throw new InvalidRequest("Unsupported operation: "+actionType);

        }

        return new JobRequest(actionType, actionScopeFor(components), getID(components));
    }

    // HTTP.method ? DELETE => anti-degraphmalize it
    private static DegraphmalizeActionType actionTypeFor(HttpRequest req)
    {
        if (HttpMethod.DELETE.equals(req.getMethod()))
            return DegraphmalizeActionType.DELETE;

        return DegraphmalizeActionType.UPDATE;
    }

    private static DegraphmalizeActionScope actionScopeFor(String[] components) {
        DegraphmalizeActionScope actionScope = DegraphmalizeActionScope.DOCUMENT;
        switch(components.length) {
            case 4:
                actionScope = DegraphmalizeActionScope.DOCUMENT;
                break;
            case 3:
                actionScope = DegraphmalizeActionScope.DOCUMENT_ANY_VERSION;
                break;
            case 2:
                actionScope = DegraphmalizeActionScope.TYPE_IN_INDEX;
                break;
            case 1:
                actionScope = DegraphmalizeActionScope.INDEX;
                break;
        }
        return actionScope;
    }


    private static ID getID(String[] components) {
        long version=0;
        String id=null;
        String type=null;
        String index=null;

        switch(components.length) {
            case 4:
                version = Long.parseLong(components[3]);
            case 3:
                id = components[2];
            case 2:
                type = components[1];
            case 1:
                index = components[0];
                break;
            default:
                throw new InvalidRequest("Invalid number of components in the request ");
        }
        return new ID(index,type,id,version);
    }
}