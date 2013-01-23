package streaming.requestmapper;

import com.google.common.base.Optional;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * * this represents a repository where channels can be stored by id.
 * How does it work?
 * We use channel groups in the background. When:
 * a channel is registered with an id, and
 *   -> the id is not in the map:
 *      -> create a channel group by that id, add the channel, and be done.
 *   -> the id is in the map:
 *      -> fetch the channel group, and add the channel.
 *
 * When a channel with a given id is removed, and
 *   -> the id is in the map:
 *     -> fetch the chanel group, and remove the channel. if the group is now empty: remove the group.
 *   -> the id is not in the map:
 *     -> what can you do?
 *
 * @author Ernst Bunders
 */
public final class ChannelContextImpl implements ChannelContext {
    private static final Map<String, ChannelGroup> repository = new ConcurrentHashMap<String, ChannelGroup>();
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelContextImpl.class);

    @Override
    public final Optional<ChannelGroup> getChannelGroup(String id) {
        if (repository.containsKey(id)) return Optional.of(repository.get(id));
        return Optional.absent();
    }

    @Override
    public final ChannelGroup addChannel(String id, Channel channel) {
        if(!repository.containsKey(id)) {
            repository.put(id, new DefaultChannelGroup(id));
            LOGGER.debug("Adding channel group with id {}", id);
        }
        repository.get(id).add(channel);
        LOGGER.debug("Adding channel with id {} to group with id {}", channel.getId(), id);
        return repository.get(id);
    }

    @Override
    public void removeChannel(Channel channel) {
        String key = null;
        for (Map.Entry<String, ChannelGroup> entry : repository.entrySet()) {
            if (entry.getValue().contains(channel)) {
                LOGGER.debug("Removing channel with id {} from channel group with id {}", channel.getId(), entry.getKey());
                entry.getValue().remove(channel);
                if (entry.getValue().size() == 0) {
                    key = entry.getKey();
                }
            }
        }

        if (key != null) {
            LOGGER.debug("Removing channel group with id {} from the context", key);
            repository.remove(key);
        }
    }
}
