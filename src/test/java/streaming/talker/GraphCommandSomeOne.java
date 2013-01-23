package streaming.talker;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.HttpChunk;
import streaming.command.GraphCommand;

public class GraphCommandSomeOne implements SomeOne<GraphCommand> {
    private final Channel channel;

    GraphCommandSomeOne(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void talk(GraphCommand message) {
        channel.write(message);
    }

    @Override
    public void done() {
        ChannelFuture future = channel.write(HttpChunk.LAST_CHUNK);
        try {
            future.await();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        channel.close();
    }
}
