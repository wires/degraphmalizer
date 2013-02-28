package dgm.degraphmalizr.recompute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dgm.ID;
import org.elasticsearch.action.index.IndexResponse;

import java.util.List;
import java.util.Map;

/**
 *
 */
public interface RecomputeResultFactory
{
    RecomputeResult recomputeSuccess(RecomputeRequest parent, IndexResponse indexResponse, JsonNode sourceDocument,
                                     ObjectNode resultDocument, Map<String,JsonNode> properties);

    RecomputeResult recomputeException(RecomputeRequest parent, Throwable exception);
    RecomputeResult recomputeExpired(RecomputeRequest parent, List<ID> expired);
    RecomputeResult recomputeFailed(RecomputeRequest parent, RecomputeResult.Status fail);
}
