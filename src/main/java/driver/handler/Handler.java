package driver.handler;

import com.google.inject.Inject;
import degraphmalizr.Degraphmalizr;
import degraphmalizr.ID;
import degraphmalizr.jobs.*;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Handler extends SimpleChannelHandler
{
    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private final Degraphmalizr degraphmalizr;

    @Inject
    public Handler(Degraphmalizr degraphmalizr)
    {
        this.degraphmalizr = degraphmalizr;
    }

    @Override
    public final void messageReceived(final ChannelHandlerContext ctx, MessageEvent e) throws Exception
    {

        if(!ID.class.isAssignableFrom(e.getMessage().getClass()))
            return;

        final ID id = (ID)e.getMessage();

        final DegraphmalizeStatus callback = new DegraphmalizeStatus()
        {
            @Override
            public void recomputeStarted(RecomputeAction action)
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
                log.error("Exception occurred: {}", result.exception());
                // send exception message upstream. We cannot simply throw the exception because this is not executed
                // in the netty selector thread
                ctx.sendUpstream(new DefaultExceptionEvent(ctx.getChannel(), result.exception()));
            }
        };

        // write the action object
        List<DegraphmalizeAction> degraphmalizeActions = degraphmalizr.degraphmalize(id, callback);
        if (degraphmalizeActions.isEmpty()) {
            ctx.getChannel().write(new NoAction()).addListener(ChannelFutureListener.CLOSE);
        } else {
            for(final DegraphmalizeAction action : degraphmalizeActions)
                ctx.getChannel().write(action);
        }
    }
}
