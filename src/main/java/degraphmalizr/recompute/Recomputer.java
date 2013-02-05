package degraphmalizr.recompute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.*;
import configuration.PropertyConfig;
import configuration.WalkConfig;
import configuration.javascript.JSONUtilities;
import degraphmalizr.ID;
import elasticsearch.QueryFunction;
import elasticsearch.ResolvedPathElement;
import graphs.GraphQueries;
import modules.bindingannotations.Fetches;
import modules.bindingannotations.Recomputes;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import trees.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class Recomputer
{
    @Inject
    Logger log;

    protected final Client client;
    protected final Graph graph;
    protected final RecomputeResultFactory factory;
    protected final ExecutorService recomputeQueue;
    protected final ExecutorService fetchQueue;
    protected final QueryFunction queryFn;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public Recomputer(Client client, Graph graph,
                      RecomputeResultFactory resultFactory,
                      @Fetches ExecutorService fetchQueue,
                      @Recomputes ExecutorService recomputeQueue,
                      QueryFunction queryFunction)
    {
        this.fetchQueue = fetchQueue;
        this.recomputeQueue = recomputeQueue;
        this.factory = resultFactory;
        this.graph = graph;
        this.client = client;
        this.queryFn = queryFunction;
    }

    /**
     * This procuedure actually performes the recompute of individual documents. It performs transformation,
     * applies walks and inserts/updates the target document.
     *
     * @param request represents the source document and recompute configuration.
     * @return the ES IndexRespons to the insert of the target document.
     */
    private RecomputeResult recompute(final RecomputeRequest request, RecomputeCallback callback)
    {
        try
        {
            return doRecompute(request,  callback);
        }
        catch (Exception e)
        {
            log.error("Unexpected exception during recompute", e);
            return factory.recomputeException(request, e);
        }
    }

    private RecomputeResult doRecompute(RecomputeRequest request, RecomputeCallback cb) throws IOException, ExecutionException, InterruptedException
    {
        // ideally, this is handled in a monad, but with this boolean we keep track of failures
        boolean isAbsent = false;

        // Now we are going to iterate over all the walks configured for this input document. For each walk:
        // - We fetch a tree of children non-recursively from our document in the inverted direction of the walk, as Graph vertices
        // - We convert the tree of vertices to a tree of ElasticSearch documents
        // - We call the reduce() method for this walk, with the tree of documents as argument.
        // - We collect the result.

        final HashMap<String, JsonNode> walkResults = new HashMap<String, JsonNode>();

        if (!request.config.walks().entrySet().isEmpty())
        {
            for (Map.Entry<String, WalkConfig> walkCfg : request.config.walks().entrySet())
            {
                // walk graph, and fetch all the children in the opposite direction of the walk
                final Tree<Pair<Edge, Vertex>> tree =
                        GraphQueries.childrenFrom(graph, request.root.vertex(), walkCfg.getValue().direction());

                // write size information to log
                if (log.isDebugEnabled())
                {
                    final int size = Iterables.size(Trees.bfsWalk(tree));
                    log.debug("Retrieving {} documents from ES", size);
                }

                // get all documents in the tree from Elasticsearch (in parallel)
                final Tree<Optional<ResolvedPathElement>> docTree = Trees.pmap(fetchQueue, queryFn, tree);

                // if some value is absent from the tree, abort the computation
                final Optional<Tree<ResolvedPathElement>> fullTree = Trees.optional(docTree);

                // TODO split various failure modes
                if (!fullTree.isPresent())
                {
                    isAbsent = true;
                    break;
                }

                // reduce each property to a value based on the walk result
                for (final Map.Entry<String, ? extends PropertyConfig> propertyCfg : walkCfg.getValue().properties().entrySet())
                    walkResults.put(propertyCfg.getKey(), propertyCfg.getValue().reduce(fullTree.get()));
            }

            // something failed, so we abort the whole re-computation
            if (isAbsent)
            {
                log.debug("Some results were absent, aborting re-computation for {}", request.root.ID());

                // TODO return list of expired nodes/IDs
                return factory.recomputeExpired(request, Collections.<ID>emptyList());
            }
        }

        // Now we are going to:
        // - fetch the current ElasticSearch document,
        // - Transform it, if transformation is required
        // - Add the walk properties
        // - Add a reference to the source document.
        // - And store it as target document type in target index.

        //todo: what if Optional.absent??
        //todo: queryFn.apply may produce null
        ResolvedPathElement root = queryFn.apply(new Pair<Edge, Vertex>(null, request.root.vertex())).get();
        // fetch the current ES document
        //todo: what if optional absent?
        final JsonNode rawDocument = objectMapper.readTree(root.getResponse().get().getSourceAsString());

        // pre-process document using javascript
        final JsonNode transformed = request.config.transform(rawDocument);

        if (!transformed.isObject())
        {
            log.debug("Root of processed document is not a JSON object (ie. it's a value), we are not adding the reduced properties");
            return factory.recomputeFailed(request, RecomputeResult.Status.SOURCE_NOT_OBJECT);
        }

        final ObjectNode document = (ObjectNode) transformed;

        // add the results to the document
        for (Map.Entry<String, JsonNode> e : walkResults.entrySet())
            document.put(e.getKey(), e.getValue());

        // write the result document to the target index
        final String targetIndex = request.config.targetIndex();
        final String targetType = request.config.targetType();
        final ID id = request.root.ID();
        final String idString = id.id();

        // write the source version to the document
        document.put("_fromSource", JSONUtilities.toJSON(objectMapper, id));

        // write document to Elasticsearch
        final String documentSource = document.toString();
        final IndexResponse ir = client.prepareIndex(targetIndex, targetType, idString).setSource(documentSource)
                .execute().actionGet();

        log.debug("Written /{}/{}/{}, version={}", new Object[]{targetIndex, targetType, idString, ir.version()});

        log.debug("Content: {}", documentSource);
        return factory.recomputeSuccess(request, ir, rawDocument, document, walkResults);
    }

}