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
 * this class initiates a stream to a graph client (Gephi, ..)
 * It registers the channel to a given id with the channelContext
 * When an optional vertex id is given, that one is streamed straight away.
 *
 * request format: /createStream/{channel id}[/{vertex id}]
 *
 * When the channel id is missing, a RequestHandlerException is thrown, resulting in an 'internal server error' response.
 *
 * @author Ernst Bunders
 */
public final class CreateStreamRequestHandler extends BaseGraphStreamerRequestHandler
{
    public  static final String PATH_REGEX = "^/createStream/(\\w)+(?:/(\\w+))?";
    private static final String ERR_MSG = "Wow! you can not call the create stream request without an id. Proper form: /createStream/{id}[/{subGraphId}]";

    public CreateStreamRequestHandler(ChannelContext channelContext, GraphUnfoldingService graphStreamingService) {
        super(channelContext, graphStreamingService);
    }

    @Override
    public void handleRequest(HttpRequest request, final Channel channel) throws RequestHandlerException {
        final List<String> params = RequestHandlerUtil.getGroups(request.getUri(), PATH_REGEX);
        final String id = getParamOrFail(params, 0, ERR_MSG);
        final Optional<String> vertexIdOption = getParamOption(params, 1);

        //add the channel to the context.
        final ChannelGroup channelGroup = getChannelContext().addChannel(id, channel);

        //now emit the response, but keep the line open
        final HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        res.setChunked(true);
        channel.write(res);

        //now, if a vertex id was given, send that to the channel group with this id.
        if (vertexIdOption.isPresent())
            getGraphStreamingService().unfoldVertex(vertexIdOption.get(), new ToChannelGroupListener(channelGroup));
    }

    @Override
    public String getPathMatchingExpression() {
        return PATH_REGEX;
    }
}
