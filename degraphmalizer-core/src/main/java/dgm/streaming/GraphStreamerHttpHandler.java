package dgm.streaming;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dgm.streaming.blueprints.GraphCommandListener;
import dgm.streaming.blueprints.StreamingGraph;
import dgm.streaming.requestmapper.ChannelContext;
import dgm.streaming.requestmapper.HttpRequestMapper;
import dgm.streaming.requestmapper.RequestHandlerException;
import dgm.streaming.requestmapper.handlers.GlobalCommandStreamRequestHandler;

import java.nio.charset.Charset;
import java.util.List;

public final class GraphStreamerHttpHandler extends SimpleChannelHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStreamerHttpHandler.class);

    private final HttpRequestMapper requestMapper;
    private final ChannelContext channelContext;
    private final StreamingGraph streamingGraph;

    public GraphStreamerHttpHandler(HttpRequestMapper requestMapper, ChannelContext channelContext, StreamingGraph streamingGraph)
    {
        this.requestMapper = requestMapper;
        this.channelContext = channelContext;
        this.streamingGraph = streamingGraph;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
    {
        LOGGER.debug("Connection received from");
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
    {
        if (HttpRequest.class.isAssignableFrom(e.getMessage().getClass()))
        {
            HttpRequest request = (HttpRequest) e.getMessage();
            Channel channel = e.getChannel();
            try
            {
                requestMapper.handleRequest(request, channel);
            }
            catch (RequestHandlerException rhe)
            {
                String errorMessage = "Can not handle request [" + request.getUri() + "]. Reason: " + rhe.getMessage();
                LOGGER.error(errorMessage);
                channel.write(createErrorResponse(rhe));
                channel.close();
            }
        }
    }

    private HttpResponse createErrorResponse(RequestHandlerException rhe)
    {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, rhe.getStatus());
        response.setContent(ChannelBuffers.wrappedBuffer(rhe.getMessage().getBytes(Charset.forName("UTF-8"))));
        return response;
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, final ChannelStateEvent event) throws Exception
    {
        //TODO: somehow this stuff should be moved into the HttpRequestMapper.RequestHandler interface, so the handlers can 'clean up their own mess'.
        //now this channel closes, remove it from the context, no more writing to it.
        channelContext.removeChannel(event.getChannel());

        //we also have to check if a streaming.requestmapper.handlers.GlobalCommandStreamRequestHandler.ChannelpushingGraphCommandListener
        //has been registered for this channel, if so: remove that listener
        List<GraphCommandListener> listener = streamingGraph.findListeners(new StreamingGraph.GraphCommandListenerFilter()
        {
            @Override
            public boolean matches(GraphCommandListener listener)
            {
                if (GlobalCommandStreamRequestHandler.ChannelpushingGraphCommandListener.class.isAssignableFrom(listener.getClass()))
                {
                    GlobalCommandStreamRequestHandler.ChannelpushingGraphCommandListener channelPushingListener = (GlobalCommandStreamRequestHandler.ChannelpushingGraphCommandListener) listener;
                    if (channelPushingListener.getChannel().equals(event.getChannel()))
                        return true;
                }
                return false;
            }
        });

        if (listener.size() > 0)
        {
            //there shouldn't be more than one
            if(listener.size() > 1)
                LOGGER.warn("There is more than one ChannelpushingGraphCommandListener registered with the StreamingGraph for channel " + event.getChannel());

            streamingGraph.removeGraphCommandListener(listener.get(0));
            LOGGER.debug("ChannelpushingGraphCommandListener removed for channel [{}] on account of the fact this channel is closing", event.getChannel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
        //TODO handle this stuff.
    }
}
