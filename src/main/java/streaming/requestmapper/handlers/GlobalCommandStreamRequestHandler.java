package streaming.requestmapper.handlers;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import streaming.blueprints.GraphCommandListener;
import streaming.blueprints.StreamingGraph;
import streaming.command.GraphCommand;
import streaming.requestmapper.HttpRequestMapper;
import streaming.requestmapper.RequestHandlerException;

/**
 * @author Ernst Bunders
 */
public class GlobalCommandStreamRequestHandler implements HttpRequestMapper.RequestHandler {

    public static final String PATH_REGEX = "^/global";
    private final StreamingGraph streamingGraph;

    public GlobalCommandStreamRequestHandler(StreamingGraph streamingGraph) {
        this.streamingGraph = streamingGraph;
    }

    @Override
    public void handleRequest(HttpRequest request, Channel channel) throws RequestHandlerException {
        streamingGraph.addGraphCommandListener(new ChannelpushingGraphCommandListener(channel));
    }

    @Override
    public String getPathMatchingExpression() {
        return PATH_REGEX;
    }

    public final class ChannelpushingGraphCommandListener implements GraphCommandListener {
        private final Channel channel;

        private ChannelpushingGraphCommandListener(Channel channel) {
            this.channel = channel;
        }

        @Override
        public void commandCreated(GraphCommand graphCommand) {
            channel.write(graphCommand);
        }

        public Channel getChannel(){
            return channel;
        }

    }
}
