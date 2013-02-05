package degraphmalizr.recompute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;
import degraphmalizr.ID;
import org.elasticsearch.action.index.IndexResponse;

import java.util.*;

public interface RecomputeResult
{
    public static interface Success
    {
        IndexResponse indexResponse();
        JsonNode sourceDocument();
        ObjectNode resultDocument();
        Map<String,JsonNode> properties();
    }

    public static enum Status
    {
        SUCCESS,
        EXCEPTION,
        EXPIRED,

        SOURCE_DOCUMENT_ABSENT,
        SOURCE_NOT_OBJECT, // source is not a json object, e.g array or value
        TARGET_INDEX_ABSENT
    }

    RecomputeRequest action();

    Status status();

    Optional<Success> success();
    Optional<Throwable> exception();
    Optional<List<ID>> expired();
}