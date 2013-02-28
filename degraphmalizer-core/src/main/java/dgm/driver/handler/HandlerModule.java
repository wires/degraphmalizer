package dgm.driver.handler;

import com.google.inject.*;
import dgm.configuration.Configuration;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpServerCodec;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import java.util.concurrent.Executor;


/**
 * This is where you configure your handlers and the pipeline
 */
public class HandlerModule extends AbstractModule
{
    @Override
    protected final void configure()
    {
        // bind to our handler
        bind(ChannelHandler.class).to(Handler.class);
    }

    @Provides
    @Singleton
    final Executor provideExecutor()
    {
        return new OrderedMemoryAwareThreadPoolExecutor(1, 1048576, 1048576);
    }

    @Provides
    @Inject
    final ChannelPipeline providePipeline(ChannelHandler handler, Executor executor, Provider<Configuration> cfg)
    {
        // construct empty pipeline
        final ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("http-codec", new HttpServerCodec());
        //pipeline.addLast("chunk-aggregator", new HttpChunkAggregator(1024 * 1024 * 2));

        // convert http request into degraphmalize requests
        pipeline.addLast("degraphmalize-decoder", new DegraphmalizeDecoder());

        // convert degraphmalize responses into http responses
        pipeline.addLast("degraphmalize-encode", new DegraphmalizeResultEncoder());

        // does the work
        pipeline.addLast("resource", handler);

        // handle exception (they always flow upstream)
        pipeline.addLast("exception-handler", new ExceptionHandler());

        return pipeline;
    }
}