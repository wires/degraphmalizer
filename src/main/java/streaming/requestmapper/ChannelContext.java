package streaming.requestmapper;

import com.google.common.base.Optional;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;

/**
 * @author Ernst Bunders
 */
public interface ChannelContext {
    public Optional<ChannelGroup> getChannelGroup(String id);

    public ChannelGroup addChannel(String id, Channel channel);

    public void removeChannel(Channel channel);

}
