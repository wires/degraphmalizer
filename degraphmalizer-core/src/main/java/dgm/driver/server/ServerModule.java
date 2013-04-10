package dgm.driver.server;

import com.google.inject.*;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

/**
 * Configure some pretty standard netty options
 */
public class ServerModule extends AbstractModule
{
    final int port;
    final String host;

    public ServerModule(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    @Override
    protected final void configure()
    {
        if (StringUtils.isEmpty(host)) {
            bind(SocketAddress.class).toInstance(new InetSocketAddress(port));
        } else {
            try {
                bind(SocketAddress.class).toInstance(new InetSocketAddress(InetAddress.getByName(host), port));
            } catch (UnknownHostException e) {
                throw new RuntimeException("Can't find address: "+host,e);
            }
        }
    }

    @Provides
    @Inject
    @Singleton
    final ServerBootstrap provideServerBootstrap(ChannelFactory channelFactory, Provider<ChannelPipeline> pipelineProvider)
    {
        final ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

        // adapt a Provider<ChannelPipeline> into a ChannelPipelineFactory
        bootstrap.setPipelineFactory(new ProviderPipelineFactory(pipelineProvider));

        bootstrap.setOption("connectTimeoutMillis", 2000);
        bootstrap.setOption("backlog", 1000);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        return bootstrap;
    }

    @Provides @Inject @Singleton
    final ChannelFactory provideChannelFactory()
    {
        // NIO channels
        return new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()
        );
    }

    // provider -> factory
    static class ProviderPipelineFactory implements ChannelPipelineFactory
    {
        final Provider<ChannelPipeline> pipeline;

        public ProviderPipelineFactory(Provider<ChannelPipeline> pipeline)
        {
            this.pipeline = pipeline;
        }

        public ChannelPipeline getPipeline() throws Exception
        {
            return pipeline.get();
        }
    }
}