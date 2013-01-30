package org.elasticsearch.plugin.degraphmalizer;

public class Change
{
    private final Action action;
    private final String type;
    private final String id;
    private final long version;

    public Change(final Action action, final String type, final String id, long version)
    {
        this.action = action;
        this.type = type;
        this.id = id;
        this.version = version;
    }

    public Action action()
    {
        return action;
    }

    public String type()
    {
        return type;
    }

    public String id()
    {
        return id;
    }

    public long version()
    {
        return version;
    }

    public static Change update(final String type, final String id, final long version)
    {
        return new Change(Action.UPDATE, type, id, version);
    }

    public static Change delete(final String type, final String id, final long version)
    {
        return new Change(Action.DELETE, type, id, version);
    }

    @Override
    public String toString() {
        return "Change{" +
                "action=" + action +
                ", type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", version=" + version +
                '}';
    }
}
