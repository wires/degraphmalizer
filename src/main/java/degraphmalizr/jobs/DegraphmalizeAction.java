package degraphmalizr.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.hash.*;
import configuration.TypeConfig;
import degraphmalizr.ID;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class DegraphmalizeAction
{
    // instance counter to 'salt' the job hashes
    protected static final AtomicLong ids = new AtomicLong();
    protected static final HashFunction hf = Hashing.md5();

    public final long salt;
    public final HashCode hash;

    protected final DegraphmalizeActionType actionType;

    protected final TypeConfig typeCfg;
    protected final ID id;

    protected JsonNode document = null;
    protected final DegraphmalizeStatus status;

    // TODO fix in Degraphmalizer.degraphmalizeJob
    public Future<JsonNode> result;

    public DegraphmalizeAction(DegraphmalizeActionType actionType, ID id, TypeConfig typeCfg, DegraphmalizeStatus callback)
    {
        this.actionType = actionType;
        this.id = id;
        this.typeCfg = typeCfg;
        this.status = callback;

        this.salt = ids.incrementAndGet();

        // compute hash
        this.hash = hf.newHasher().putLong(salt).putString(typeCfg.name()).putString(id.toString()).hash(); // TODO: Job hashing broken
    }

    public final HashCode hash()
    {
        return hash;
    }

    public final ID id()
    {
        return id;
    }

    public DegraphmalizeActionType type() {
        return actionType;
    }

    public final TypeConfig typeConfig()
    {
        return typeCfg;
    }

    public final DegraphmalizeStatus status()
    {
        return status;
    }

    // TODO wrong
    public final void setDocument(JsonNode document)
    {
        this.document = document;
    }

    public final JsonNode document()
    {
        return document;
    }

    public final Future<JsonNode> resultDocument()
    {
        return result;
    }
}
