package streaming.requestmapper.handlers;

import com.google.common.base.Optional;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.*;
import streaming.requestmapper.ChannelContext;
import streaming.requestmapper.RequestHandlerException;
import streaming.requestmapper.RequestHandlerUtil;
import streaming.requestmapper.ToChannelGroupListener;
import streaming.service.GraphUnfoldingService;

import java.util.List;

/**
 * This class is supposed to handle the command for adding the GraphCommands that represent a given vertex (and it's edges) to
 * an existing open channel.
 * The request path should contain both the id by which the Channel is stored in the ChannelContextImpl, and the id
 * of the vertex we want to stream.
 *
 * request format: /updateStream/{channel id}[/{vertex id}]
 *
 * When the channel id does not exist, a 'bad request' response is returned
 *
 * @author Ernst Bunders
 */
public final class UpdateStreamRequestHandler extends BaseGraphStreamerRequestHandler
{

    public static final String PATH_REGEX = "^/updateStream/(\\w)+/(\\w+)";
    private static final String ERR_MSG = "Wow! you can not call the update stream request without an id. proper form: /updateStream/{id}/{subGraphId}";


    public UpdateStreamRequestHandler(ChannelContext channelContext, GraphUnfoldingService graphStreamingService) {
        super(channelContext, graphStreamingService);
    }

    @Override
    public void handleRequest(HttpRequest request, final Channel channel) throws RequestHandlerException {
        final List<String> params = RequestHandlerUtil.getGroups(request.getUri(), PATH_REGEX);
        final String id = getParamOrFail(params, 0, ERR_MSG);
        final String vertexId = getParamOrFail(params, 1, ERR_MSG);

        final Optional<ChannelGroup> channelOptional = channelContext.getChannelGroup(id);
        if(!channelOptional.isPresent())
            throw new RequestHandlerException("There is no command stream with id " + id + "\n", HttpResponseStatus.BAD_REQUEST);

        //handle the http stuff
        final HttpResponse res =  new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        channel.write(res);
        channel.close();

        //handle the GraphCommand stuff
        graphStreamingService.unfoldVertex(vertexId, new ToChannelGroupListener(channelOptional.get()));
    }

    @Override
    public String getPathMatchingExpression() {
        return PATH_REGEX;
    }
}
