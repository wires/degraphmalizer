package driver.server;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import java.net.SocketAddress;

/**
 *
 */
public class Server extends AbstractIdleService
{
    final ChannelGroup allChannels = new DefaultChannelGroup();
    final SocketAddress address;
    final ServerBootstrap bootstrap;
    final ChannelFactory factory;

    @Inject
    public Server(ServerBootstrap bootstrap, ChannelFactory factory, SocketAddress address)
    {
        this.address = address;
        this.bootstrap = bootstrap;
        this.factory = factory;
    }

    @Override
    protected void startUp() throws Exception
    {
        //log.info("Server started at {}", address);
        final Channel channel = bootstrap.bind(address);
        allChannels.add(channel);
    }

    @Override
    protected void shutDown() throws Exception
    {
        allChannels.close().awaitUninterruptibly();
        factory.releaseExternalResources();
    }
}
