package dgm.driver.handler;

import com.google.inject.Inject;
import dgm.Degraphmalizr;
import dgm.degraphmalizr.degraphmalize.*;
import dgm.degraphmalizr.recompute.RecomputeRequest;
import dgm.degraphmalizr.recompute.RecomputeResult;
import org.jboss.netty.channel.*;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

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

        if(!JobRequest.class.isAssignableFrom(e.getMessage().getClass()))
            return;

        final JobRequest jobRequest = (JobRequest)e.getMessage();

        final DegraphmalizeStatus callback = new DegraphmalizeStatus()
        {
            @Override
            public void recomputeStarted(RecomputeRequest action)
            {
                log.info("recompute started");
//                ctx.getChannel().write(action);
            }

            @Override
            public void recomputeComplete(RecomputeResult result)
            {
                log.info("recompute completed");
//                ctx.getChannel().write(result);
            }

            @Override
            public void complete(DegraphmalizeResult result)
            {
                log.info("completed degraphmalization");

                // write completion message and close channel
                ctx.getChannel().write(result).addListener(ChannelFutureListener.CLOSE);
            }

            @Override
            public void exception(DegraphmalizeResult result)
            {
                // send exception message upstream. We cannot simply throw the exception because this is not executed
                // in the netty selector thread
                ctx.sendUpstream(new DefaultExceptionEvent(ctx.getChannel(), result.exception()));
            }
        };

        // write the action object
        final DegraphmalizeAction action = degraphmalizr.degraphmalize(jobRequest.actionType(), jobRequest.actionScope(), jobRequest.id(), callback);
        //ctx.getChannel().write(action);
    }
}
