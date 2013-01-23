package driver.handler;

import com.google.inject.Inject;
import degraphmalizr.Degraphmalizr;
import degraphmalizr.ID;
import degraphmalizr.jobs.*;
import org.jboss.netty.channel.*;

/**
 *
 */
public class Handler extends SimpleChannelHandler
{
    private final Degraphmalizr degraphmalizr;

    @Inject
    public Handler(Degraphmalizr degraphmalizr)
    {
        this.degraphmalizr = degraphmalizr;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent e) throws Exception
    {

        if(!ID.class.isAssignableFrom(e.getMessage().getClass()))
            return;

        final ID id = (ID)e.getMessage();

        final DegraphmalizeStatus callback = new DegraphmalizeStatus()
        {
            @Override
            public void recomputeStarted(RecomputeAction action)
            {
                System.err.println("recompute started");
//                ctx.getChannel().write(action);
            }

            @Override
            public void recomputeComplete(RecomputeResult result)
            {
                System.err.println("recompute completed");
//                ctx.getChannel().write(result);
            }

            @Override
            public void complete(DegraphmalizeResult result)
            {
                System.err.println("completed degraphmalization");

                // write completion message and close channel
                ctx.getChannel().write(result).addListener(ChannelFutureListener.CLOSE);
            }

            @Override
            public void exception(DegraphmalizeResult result)
            {
                System.err.println("exception occured " + result.exception());
                // send exception message upstream. We cannot simply throw the exception because this is not executed
                // in the netty selector thread
                ctx.sendUpstream(new DefaultExceptionEvent(ctx.getChannel(), result.exception()));
            }
        };

        final DegraphmalizeAction action = degraphmalizr.degraphmalize(id, callback);

        // write the action object
        ctx.getChannel().write(action);
    }
}
