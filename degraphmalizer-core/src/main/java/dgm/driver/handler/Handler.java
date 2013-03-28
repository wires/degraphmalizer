package dgm.driver.handler;

import com.google.inject.Inject;
import dgm.Degraphmalizr;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeCallback;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeRequest;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeResult;
import dgm.degraphmalizr.degraphmalize.JobRequest;
import dgm.exceptions.DegraphmalizerException;
import org.jboss.netty.channel.*;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

import java.util.concurrent.Future;

public class Handler extends SimpleChannelHandler
{
    @InjectLogger
    private static Logger log;

    private final Degraphmalizr degraphmalizr;

    @Inject
    public Handler(Degraphmalizr degraphmalizr)
    {
        this.degraphmalizr = degraphmalizr;
    }

    @Override
    public final void messageReceived(final ChannelHandlerContext ctx, MessageEvent e) throws Exception
    {

        if (!JobRequest.class.isAssignableFrom(e.getMessage().getClass()))
            return;

        final JobRequest jobRequest = (JobRequest) e.getMessage();

        final DegraphmalizeCallback callback = new DegraphmalizeCallback()
        {
            @Override
            public void started(DegraphmalizeRequest request)
            {
                log.info("Started degraphmalization");
            }

            @Override
            public void complete(DegraphmalizeResult result)
            {
                // write completion message and close channel
                ctx.getChannel().write(result).addListener(ChannelFutureListener.CLOSE);
            }

            @Override
            public void failed(DegraphmalizerException exception)
            {
                // send exception message upstream. We cannot simply throw the exception because this is not executed
                // in the netty selector thread
                ctx.sendUpstream(new DefaultExceptionEvent(ctx.getChannel(), exception));
            }
        };

        final Future<DegraphmalizeResult> result = degraphmalizr.degraphmalize(jobRequest.actionType(), jobRequest.actionScope(), jobRequest.id(), callback);
    }
}
