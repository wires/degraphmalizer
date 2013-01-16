package org.elasticsearch.plugin.degraphmalizer;

public class GraphChange
{
    private GraphAction graphAction;
    private String index;
    private String type;
    private String id;
    private long version;

    public GraphChange(final GraphAction graphAction, final String index, final String type, final String id, long version)
    {
        this.graphAction = graphAction;
        this.index = index;
        this.type = type;
        this.id = id;
        this.version = version;
    }

    public GraphAction action()
    {
        return graphAction;
    }

    public String index()
    {
        return index;
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

    public static GraphChange update(final String index, final String type, final String id, final long version)
    {
        return new GraphChange(GraphAction.UPDATE, index, type, id, version);
    }

    public static GraphChange delete(final String index, final String type, final String id, final long version)
    {
        return new GraphChange(GraphAction.DELETE, index, type, id, version);
    }
}
