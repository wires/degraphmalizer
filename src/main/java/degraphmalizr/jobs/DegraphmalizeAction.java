package degraphmalizr.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.hash.*;
import configuration.IndexConfig;
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

    protected final TypeConfig typeCfg;
    protected final ID id;

    protected JsonNode document = null;
    protected final DegraphmalizeStatus status;

    // TODO fix in Degraphmalizer.degraphmalizeJob
    public Future<JsonNode> result;

    public DegraphmalizeAction(TypeConfig typeCfg, ID id, DegraphmalizeStatus callback)
    {
        this.typeCfg = typeCfg;
        this.id = id;

        this.status = callback;

        this.salt = ids.incrementAndGet();

        // compute hash
        this.hash = hf.newHasher().putLong(salt).putString(typeCfg.name()).putString(id.toString()).hash(); // TODO: Job hashing broken
    }

    public HashCode hash()
    {
        return hash;
    }

    public ID id()
    {
        return id;
    }

    public TypeConfig type()
    {
        return typeCfg;
    }

    public DegraphmalizeStatus status()
    {
        return status;
    }

    // TODO wrong
    public void setDocument(JsonNode document)
    {
        this.document = document;
    }

    public JsonNode document()
    {
        return document;
    }

    public Future<JsonNode> resultDocument()
    {
        return result;
    }
}
