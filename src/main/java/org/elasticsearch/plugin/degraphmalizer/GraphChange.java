package org.elasticsearch.plugin.degraphmalizer;

public class GraphChange
{
    private final GraphAction graphAction;
    private final String type;
    private final String id;
    private final long version;

    public GraphChange(final GraphAction graphAction, final String type, final String id, long version)
    {
        this.graphAction = graphAction;
        this.type = type;
        this.id = id;
        this.version = version;
    }

    public GraphAction action()
    {
        return graphAction;
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

    public static GraphChange update(final String type, final String id, final long version)
    {
        return new GraphChange(GraphAction.UPDATE, type, id, version);
    }

    public static GraphChange delete(final String type, final String id, final long version)
    {
        return new GraphChange(GraphAction.DELETE, type, id, version);
    }

    @Override
    public String toString() {
        return "GraphChange{" +
                "graphAction=" + graphAction +
                ", type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", version=" + version +
                '}';
    }
}
