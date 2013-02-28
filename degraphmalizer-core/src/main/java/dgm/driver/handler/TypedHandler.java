package dgm.driver.handler;

public abstract class TypedHandler<T>
{
    private final Class<T> type;

    public TypedHandler(Class<T> type)
    {
        this.type = type;
    }

    public final boolean handleEvent(Object msg) throws Exception
    {
        // only handle messages
        if (! type.isAssignableFrom(msg.getClass()))
            return false;

        // and then only message of the type that we are interested in
        return handle((T)msg);
    }

    protected abstract boolean handle(T msg);
}