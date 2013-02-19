package streaming;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import streaming.blueprints.StreamingGraph;
import streaming.codec.AddCRLFDecoder;
import streaming.codec.GraphCommandToJsonEncoder;
import streaming.codec.ToHttpChunkEncoder;
import streaming.requestmapper.*;
import streaming.requestmapper.handlers.CreateStreamRequestHandler;
import streaming.requestmapper.handlers.GlobalCommandStreamRequestHandler;
import streaming.requestmapper.handlers.UpdateStreamRequestHandler;
import streaming.service.GraphUnfoldingService;
import streaming.service.GraphUnfoldingServiceImpl;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class GraphStreamerServer {

    public GraphStreamerServer(ChannelContext channelContext, GraphUnfoldingService graphStreamingService, StreamingGraph streamingGraph) {
        this.channelContext = channelContext;
        this.graphStreamingService = graphStreamingService;

        this.streamingGraph = streamingGraph;
    }

    public static void main(String[] args) throws Exception {
        final RandomGraph rg = new RandomGraph(1000, 0.8f);
        new GraphStreamerServer(new ChannelContextImpl(), new GraphUnfoldingServiceImpl(rg), null/*TODO*/).start();
    }

    private final ChannelContext channelContext;
    private final GraphUnfoldingService graphStreamingService;
    private final StreamingGraph streamingGraph;

    public void start() {
        ChannelFactory factory =
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool());

        ServerBootstrap bootstrap = new ServerBootstrap(factory);
        bootstrap.getPipeline().addLast("httpDecoder", new HttpRequestDecoder());
        bootstrap.getPipeline().addLast("httpEncoder", new HttpResponseEncoder());

        bootstrap.getPipeline().addLast("StringToChunked", new ToHttpChunkEncoder()); // Encode message as chunk
        bootstrap.getPipeline().addLast("crlfAppender", new AddCRLFDecoder()); // Adds {0a, 0d} for Gephi
        bootstrap.getPipeline().addLast("stringEnconder", new StringEncoder()); // creates ChannelBuffer from String
        bootstrap.getPipeline().addLast("TojsonStringEncoder", new GraphCommandToJsonEncoder()); // creates json (String) from GraphCommand
        bootstrap.getPipeline().addLast("handler", new GraphStreamerHttpHandler(createRequestMapper(), channelContext, streamingGraph));

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(9090));
    }

    private HttpRequestMapper createRequestMapper() {
        return new HttpRequestMapper()
                .addMapping(new CreateStreamRequestHandler(channelContext, graphStreamingService), HttpMethod.GET)
                .addMapping(new UpdateStreamRequestHandler(channelContext, graphStreamingService), HttpMethod.GET)
                .addMapping(new GlobalCommandStreamRequestHandler(streamingGraph));
    }
}