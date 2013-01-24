package trees;

import org.elasticsearch.action.get.GetResponse;

public final class QueryResponse
{
    final boolean hasExpired;
    final GetResponse response;

    private QueryResponse(boolean expired, GetResponse response)
    {
        this.hasExpired = expired;
        this.response = response;
    }

    /**
     * Call this method when the specific version was no longer available
     */
    public static QueryResponse expired()
    {
        return new QueryResponse(true, null);
    }

    /**
     * Call this method when no ID was defined for the vertex.
     */
    public static QueryResponse noID()
    {
        return new QueryResponse(false, null);
    }

    public boolean hasExpired()
    {
        return this.hasExpired;
    }

    public boolean hasResponse()
    {
        return this.response != null;
    }

    public GetResponse response()
    {
        return response;
    }
}
