package streaming.codec;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import streaming.command.GraphCommand;
import streaming.command.GraphNode;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class GraphCommandToJsonEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof GraphCommand)) {
            return msg;
        } else {
            GraphCommand graphCommand = (GraphCommand) msg;
            return encodeGraphCommand(graphCommand);
        }
    }

    private String encodeGraphCommand(final GraphCommand graphCommand) throws Exception {
        Map<String, Object> json = new HashMap<String, Object>();
        Map<String, Object> nodes = new HashMap<String, Object>();

        for (GraphNode graphNode : graphCommand.getNodes()) {
            nodes.put(graphNode.getName(), graphNode.getProperties());
        }

        json.put(graphCommand.getCommandType().toString(), nodes);

        ObjectMapper objectMapper = new ObjectMapper();
        StringWriter writer = new StringWriter();

        objectMapper.writeValue(writer, json);

        return writer.toString();
    }
}
