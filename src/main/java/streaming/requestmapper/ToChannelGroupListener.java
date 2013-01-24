package streaming.requestmapper;

import org.jboss.netty.channel.group.ChannelGroup;
import streaming.blueprints.GraphCommandListener;
import streaming.command.GraphCommand;

/**
 * @author Ernst Bunders
 */
public class ToChannelGroupListener implements GraphCommandListener {
    private final ChannelGroup channelGroup;

    public ToChannelGroupListener(ChannelGroup channel) {
        this.channelGroup = channel;
    }

    @Override
    public final void commandCreated(GraphCommand graphCommand) {
        channelGroup.write(graphCommand);
    }
}
