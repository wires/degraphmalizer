package degraphmalizr.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import degraphmalizr.ID;
import org.elasticsearch.action.index.IndexResponse;

import java.util.List;
import java.util.Map;

/**
 *
 */
public interface RecomputeResultFactory
{
    RecomputeResult recomputeSuccess(RecomputeAction parent, IndexResponse indexResponse, ObjectNode sourceDocument,
                                     ObjectNode resultDocument, Map<String,JsonNode> properties);

    RecomputeResult recomputeException(RecomputeAction parent, Throwable exception);
    RecomputeResult recomputeExpired(RecomputeAction parent, List<ID> expired);
    RecomputeResult recomputeFailed(RecomputeAction parent, RecomputeResult.Status fail);
}
