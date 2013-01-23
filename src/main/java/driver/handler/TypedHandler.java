package driver.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;

/**
 *
 */
public abstract class TypedHandler<T>
{
    final private Class<T> type;

    public TypedHandler(Class<T> type)
    {
        this.type = type;
    }

    public boolean handleEvent(Object msg) throws Exception
    {
        // only handle messages
        if (! type.isAssignableFrom(msg.getClass()))
            return false;

        // and then only message of the type that we are interested in
        return handle((T)msg);
    }

    abstract protected boolean handle(T msg);
}