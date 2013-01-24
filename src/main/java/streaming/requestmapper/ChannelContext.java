package streaming.requestmapper;

import com.google.common.base.Optional;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;

/**
 * @author Ernst Bunders
 */
public interface ChannelContext
{
    Optional<ChannelGroup> getChannelGroup(String id);

    ChannelGroup addChannel(String id, Channel channel);

    void removeChannel(Channel channel);
}
