package degraphmalizr.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;
import degraphmalizr.ID;
import degraphmalizr.jobs.*;
import org.elasticsearch.action.index.IndexResponse;

import java.util.List;
import java.util.Map;

/**
 * Construct recompute result object
 */
public class RRFactory implements RecomputeResultFactory
{

    /**
     * Recompute was successful
     *
     * @param parent The action associated with this result
     * @param indexResponse ES index response
     * @param sourceDocument The source document (before transformation)
     * @param resultDocument The resulting document (after transformation and appending properties)
     * @param properties The computed properties
     */
    @Override
    public RecomputeResult recomputeSuccess(RecomputeAction parent, IndexResponse indexResponse,
                                            ObjectNode sourceDocument, ObjectNode resultDocument,
                                            Map<String, JsonNode> properties)
    {
        final RecomputeSuccess success = new RecomputeSuccess(indexResponse, sourceDocument, resultDocument,
                properties);

        return new RecomputeResultImpl(parent, success);
    }

    /**
     * Recompute failed with an unknown exception
     *
     * @param parent The action associated with this result
     * @param exception The exception causing the failure.
     */
    @Override
    public RecomputeResult recomputeException(RecomputeAction parent, Throwable exception)
    {
        return new RecomputeResultImpl(parent, exception);
    }

    /**
     * Some (multiple?) nodes in the graph has versions not equal to the versions in elasticsearch.
     *
     * This means that the ES index was modified before the degraphmalizer processed this change and the
     * query has expired. In order for this recompute to complete, one must start degraphmalize jobs for
     * all IDs in the expired list.
     *
     * @param parent The action associated with this result
     * @param expired
     * @return
     */
    @Override
    public RecomputeResult recomputeExpired(RecomputeAction parent, List<ID> expired)
    {
        return new RecomputeResultImpl(parent, expired);
    }

    /**
     * Recompute didn't complete, for some known reason.
     *
     * @param parent The action associated with this result
     * @param status Reason why it failed
     */
    @Override
    public RecomputeResult recomputeFailed(RecomputeAction parent, RecomputeResult.Status status)
    {
        return new RecomputeResultImpl(parent, status);
    }

    // immutable container class, holding "success" data.
    private class RecomputeSuccess implements RecomputeResult.Success
    {
        final IndexResponse ir;
        final ObjectNode source;
        final ObjectNode result;
        final Map<String, JsonNode> properties;

        public RecomputeSuccess(IndexResponse ir, ObjectNode source, ObjectNode result, Map<String, JsonNode> properties)
        {
            this.ir = ir;
            this.source = source;
            this.result = result;
            this.properties = properties;
        }

        @Override
        public IndexResponse indexResponse()
        {
            return ir;
        }

        @Override
        public ObjectNode sourceDocument()
        {
            return source;
        }

        @Override
        public ObjectNode resultDocument()
        {
            return result;
        }

        @Override
        public Map<String, JsonNode> properties()
        {
            return properties;
        }
    }

    // immutable container class that holds a recompute result
    private class RecomputeResultImpl implements RecomputeResult
    {
        final RecomputeAction action;
        final Optional<Throwable> exception;
        final Optional<Success> success;
        final Optional<List<ID>> expired;
        final Optional<Status> status;

        // Recompute was successful
        private RecomputeResultImpl(RecomputeAction parent, Success success)
        {
            this.action = parent;
            this.success = Optional.of(success);
            this.exception = Optional.absent();
            this.expired = Optional.absent();
            this.status = Optional.absent();
        }

        // Recompute failed with an unknown exception
        private RecomputeResultImpl(RecomputeAction parent, Throwable ex)
        {
            this.action = parent;
            this.exception = Optional.of(ex);
            this.success = Optional.absent();
            this.expired = Optional.absent();
            this.status = Optional.absent();
        }

        /**
         * Recompute didn't complete, because some vertices in the graph have expired data
         *
         * @param parent
         * @param expired
         */
        private RecomputeResultImpl(RecomputeAction parent, List<ID> expired)
        {
            this.action = parent;
            this.expired = Optional.of(expired);
            this.exception = Optional.absent();
            this.success = Optional.absent();
            this.status = Optional.absent();
        }

        private RecomputeResultImpl(RecomputeAction parent, Status status)
        {
            if(status == Status.SUCCESS)
                throw new IllegalArgumentException("Please use the constructor (RecomputeAction, RecomputeSuccess)");

            if(status == Status.EXPIRED)
                throw new IllegalArgumentException("Please use the constructor (RecomputeAction, List<ID> expired)");

            if(status == Status.EXCEPTION)
                throw new IllegalArgumentException("Please use the constructor (RecomputeAction, Throwable exception)");

            this.action = parent;

            this.status = Optional.of(status);
            this.success = Optional.absent();
            this.exception = Optional.absent();
            this.expired = Optional.absent();
        }

        @Override
        public RecomputeAction action()
        {
            return action;
        }

        @Override
        public Optional<Success> success()
        {
            return success;
        }

        @Override
        public Optional<Throwable> exception()
        {
            return exception;
        }

        @Override
        public Optional<List<ID>> expired()
        {
            return expired;
        }

        @Override
        public Status status()
        {
            if(success.isPresent())
                return Status.SUCCESS;

            if(exception.isPresent())
                return Status.EXCEPTION;

            if(expired.isPresent())
                return Status.EXPIRED;

            return status.get();
        }
    }
}