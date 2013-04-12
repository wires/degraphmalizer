package dgm.degraphmalizr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import dgm.*;
import dgm.configuration.Configuration;
import dgm.configuration.Configurations;
import dgm.configuration.TypeConfig;
import dgm.degraphmalizr.degraphmalize.*;
import dgm.degraphmalizr.recompute.RecomputeCallback;
import dgm.degraphmalizr.recompute.RecomputeRequest;
import dgm.degraphmalizr.recompute.RecomputeResult;
import dgm.degraphmalizr.recompute.Recomputer;
import dgm.exceptions.*;
import dgm.graphs.BlueprintsSubgraphManager;
import dgm.graphs.Subgraphs;
import dgm.modules.bindingannotations.Degraphmalizes;
import dgm.modules.bindingannotations.Fetches;
import dgm.modules.bindingannotations.Recomputes;
import dgm.modules.elasticsearch.QueryFunction;
import dgm.trees.Pair;
import dgm.trees.Tree;
import dgm.trees.Trees;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static dgm.degraphmalizr.degraphmalize.DegraphmalizeRequestScope.DOCUMENT;

public class Degraphmalizer implements Degraphmalizr
{
    @InjectLogger
    Logger log;

    protected final Client client;

    protected final Graph graph;
    protected final SubgraphManager subgraphmanager;

    protected final ExecutorService degraphmalizeQueue;
    protected final ExecutorService recomputeQueue;
    protected final ExecutorService fetchQueue;

    protected final QueryFunction queryFn;
    protected final Recomputer recomputer;

    protected final Provider<Configuration> cfgProvider;

    final ObjectMapper objectMapper;

    @Inject
    public Degraphmalizer(Client client, SubgraphManager subgraphmanager, Graph graph,
                          @Degraphmalizes ExecutorService degraphmalizeQueue,
                          @Fetches ExecutorService fetchQueue,
                          @Recomputes ExecutorService recomputeQueue,
                          QueryFunction queryFunction,
                          ObjectMapper objectMapper,
                          Recomputer recomputer,
                          Provider<Configuration> configProvider)
	{
        this.fetchQueue = fetchQueue;
        this.recomputeQueue = recomputeQueue;
        this.degraphmalizeQueue = degraphmalizeQueue;
        this.graph = graph;
        this.subgraphmanager = subgraphmanager;
        this.client = client;
        this.recomputer = recomputer;
        this.cfgProvider = configProvider;
        this.queryFn = queryFunction;
        this.objectMapper = objectMapper;
	}

    @Override
    public final Future<DegraphmalizeResult> degraphmalize(DegraphmalizeRequestType requestType, DegraphmalizeRequestScope requestScope, ID id, DegraphmalizeCallback callback)
    {
        // find all matching configurations
        final Iterable<TypeConfig> configs = Configurations.configsFor(cfgProvider.get(), id.index(), id.type());

        // we cannot handle this request!
        if(Iterables.isEmpty(configs))
            throw new NoConfiguration(id);

        // construct the action object
        final DegraphmalizeRequest action = new DegraphmalizeRequest(requestType, requestScope, id, configs);

        // convert object into task and queue
        return degraphmalizeQueue.submit(degraphmalizeJob(action, callback));
    }

    private Callable<DegraphmalizeResult> degraphmalizeJob(final DegraphmalizeRequest action, final DegraphmalizeCallback callback)
    {
        return new Callable<DegraphmalizeResult>()
        {
            @Override
            public DegraphmalizeResult call() throws Exception
            {
                try
                {
                    final DegraphmalizeRequestType requestType = action.type();
                    callback.started(action);
                    DegraphmalizeResult result;
                    switch (requestType)
                    {
                        case UPDATE:
                            result = doUpdate(action);
                            break;
                        case DELETE:
                            result = doDelete(action);
                            break;
                        default:
                            throw new UnreachableCodeReachedException();
                    }
                    // collect all recompute states.
                    for (Future<RecomputeResult> recomputeResultFuture : result.results())
                    {
                        try {
                            recomputeResultFuture.get();
                        } catch (ExecutionException e) {
                            if (e.getCause() instanceof DegraphmalizerException) {
                                DegraphmalizerException de = (DegraphmalizerException)(e.getCause());
                                if (de.severity()== DegraphmalizerException.Severity.ERROR) {
                                    throw(de);
                                }
                            } else {
                                throw e;
                            }
                        }
                    }
                    callback.complete(result);
                    return result;
                } catch (final DegraphmalizerException e)
                {
                    callback.failed(e);
                    throw e;
                } catch (final Exception e)
                {
                    WrappedException we = new WrappedException(e);
                    callback.failed(we);
                    throw we;
                }
            }
        };
    }

    private boolean inList(RecomputeRequest r, List<RecomputeRequest> rs)
    {
        for(RecomputeRequest q : rs)
        {
            final boolean equalId = q.root.id().equals(r.root.id());
            final boolean equalIndexConfig = q.config.targetIndex().equals(r.config.targetIndex());
            final boolean equalTypeConfig = q.config.targetType().equals(r.config.targetType());
            if(equalId && equalIndexConfig && equalTypeConfig)
                return true;
        }

        return false;
    }

    private List<RecomputeRequest> determineRecomputeActionsOrEmpty(DegraphmalizeRequest action)
    {
        try
        {
            return determineRecomputeActions(action);
        }
        catch(NotFoundInGraphException e)
        {
            return Collections.emptyList();
        }
    }

    private DegraphmalizeResult doUpdate(DegraphmalizeRequest action) throws Exception
    {
        log.info("Processing update request for id {} scope {} ", action.id(), action.scope());

        List<Future<RecomputeResult>> results;
        switch (action.scope())
        {
            case INDEX:
                Iterable<Vertex> vertexIterator = GraphUtilities.findVerticesInIndex(graph, action.id().index());
                results = updateDocuments(vertexIterator, action);
                break;
            case TYPE_IN_INDEX:
                Iterable<Vertex> vertexIterator2 = GraphUtilities.findVerticesInIndex(graph, action.id().index(), action.id().type());
                results = updateDocuments(vertexIterator2, action);
                break;
            case DOCUMENT_ANY_VERSION:
                Vertex vertex = GraphUtilities.resolveVertex(objectMapper, graph, action.id());
                results = updateDocument(createDocumentRequestForVertex(action.type(), vertex));
                break;
            case DOCUMENT:
                results = updateDocument(action);
                break;
            default:
                throw new UnreachableCodeReachedException();
        }
        return new DegraphmalizeResult(action.id(), results);
    }

    private List<Future<RecomputeResult>> updateDocuments(Iterable<Vertex> iterator, DegraphmalizeRequest action) throws ExecutionException, InterruptedException, IOException
    {
        final List<Future<RecomputeResult>> results = new ArrayList<Future<RecomputeResult>>();
        for (Vertex vertex : iterator)
        {
            results.addAll(updateDocument(createDocumentRequestForVertex(action.type(), vertex)));
        }
        return results;
    }

    private List<Future<RecomputeResult>> updateDocument(DegraphmalizeRequest action) throws IOException, ExecutionException, InterruptedException
    {
        if (!action.scope().equals(DOCUMENT))
        {
            throw new InvalidRequest("Action : " + action.type() + " is not valid for a scope of " + action.scope());
        }

        // Get document from elasticsearch
        final JsonNode jsonNode = getDocument(action.id());

        // couldn't find source document, so we are done
        if (jsonNode == null)
        {
            return Collections.emptyList();
        } else
        {
            // find all document connected to this document before changing the graph
            final List<RecomputeRequest> pre = determineRecomputeActionsOrEmpty(action);

            // update the graph
            generateSubgraph(action, jsonNode);

            final List<RecomputeRequest> post = determineRecomputeActions(action);

            // add all the missing requests from pre to post
            for (RecomputeRequest r : pre)
                if (!inList(r, post))
                    post.add(r);

            logRecomputes(action.id(), post);
            return recomputeAffectedDocuments(post);
        }
    }

    private void logRecomputes(ID id, List<RecomputeRequest> recomputeRequests)
    {
        // fn = toString . id . root
        final Function<RecomputeRequest, String> fn = new Function<RecomputeRequest, String>()
        {
            @Override public String apply(@Nullable RecomputeRequest input) {
                return "\n\t" + input.root.id().toString();
            }
        };

        // intercalate "," $ map fn recomputeRequests
        final String ids = Joiner.on("").join(Lists.transform(recomputeRequests, fn));

        log.info("Degraphmalize request for {} triggered compute of: {}", id, ids);
    }

    private DegraphmalizeRequest createDocumentRequestForVertex(DegraphmalizeRequestType degraphmalizeRequestType, Vertex vertex)
    {
        ID id = GraphUtilities.getID(objectMapper, vertex);
        Iterable<TypeConfig> typeConfigs = Configurations.configsFor(cfgProvider.get(), id.index(), id.type());
        return new DegraphmalizeRequest(degraphmalizeRequestType, DOCUMENT, id, typeConfigs);
    }

    // TODO refactor (dubbeling met doUpdate DGM-44)
    private DegraphmalizeResult doDelete(DegraphmalizeRequest action) throws Exception
    {
        log.info("Processing delete request for id {} scope {} ", action.id(), action.scope());

        List<Future<RecomputeResult>> results;
        switch (action.scope())
        {
            case INDEX:
                Iterable<Vertex> vertexIterator = GraphUtilities.findVerticesInIndex(graph, action.id().index());
                results = deleteDocuments(vertexIterator, action);
                break;
            case TYPE_IN_INDEX:
                Iterable<Vertex> vertexIterator2 = GraphUtilities.findVerticesInIndex(graph, action.id().index(), action.id().type());
                results = deleteDocuments(vertexIterator2, action);
                break;
            case DOCUMENT_ANY_VERSION:
                Vertex vertex = GraphUtilities.resolveVertex(objectMapper, graph, action.id());
                results = deleteDocument(createDocumentRequestForVertex(action.type(), vertex));
                break;
            case DOCUMENT:
                results = deleteDocument(action);
                break;
            default:
                throw new UnreachableCodeReachedException();
        }
        return new DegraphmalizeResult(action.id(), results);
    }

    private List<Future<RecomputeResult>> deleteDocuments(Iterable<Vertex> iterator, DegraphmalizeRequest action) throws ExecutionException, InterruptedException
    {
        final List<Future<RecomputeResult>> results = new ArrayList<Future<RecomputeResult>>();
        for (Vertex vertex : iterator)
        {
            results.addAll(deleteDocument(createDocumentRequestForVertex(action.type(), vertex)));
        }
        return results;
    }

    private List<Future<RecomputeResult>> deleteDocument(DegraphmalizeRequest action) throws ExecutionException, InterruptedException
    {
        if (!action.scope().equals(DOCUMENT))
        {
            throw new InvalidRequest("Delete a document is not valid for a scope of " + action.scope());
        }

        List<RecomputeRequest> recomputeRequests = determineRecomputeActions(action);
        // TODO refactor refactor!
        List<ID> verticesDeleted = ((BlueprintsSubgraphManager) subgraphmanager).findVertexIDsAffectedByDelete(action.id());

        subgraphmanager.deleteSubgraph(action.id());

        recomputeRequests = removeDeletedVerticesFromRequest(verticesDeleted, recomputeRequests);

        logRecomputes(action.id(), recomputeRequests);

        return recomputeAffectedDocuments(recomputeRequests);
    }

    private List<RecomputeRequest> removeDeletedVerticesFromRequest(final List<ID> deleted, List<RecomputeRequest> requests)
    {
        final Set<ID> deletedIDs = new HashSet<ID>(deleted);
        Iterables.removeIf(requests, new Predicate<RecomputeRequest>()
        {
            @Override
            public boolean apply(RecomputeRequest input)
            {
                return deletedIDs.contains(input.root.id());
            }
        });
        return requests;
    }

    private JsonNode getDocument(ID id) throws InterruptedException, ExecutionException, IOException
    {
        // get the source document from Elasticsearch
        final GetResponse resp = client.prepareGet(id.index(), id.type(), id.id()).execute().get();

        if (!resp.exists())
            return null;

        // TODO: shouldn't this be: resp.version() > id.version()
        log.debug("Request has version " + id.version() + " and current es document has version " + resp.version());
        if (resp.version() != id.version())
            throw new ExpiredException(id.version(resp.version()));

        return objectMapper.readTree(resp.getSourceAsString());
    }

    private void generateSubgraph(DegraphmalizeRequest action, JsonNode document)
    {
        GraphUtilities.dumpGraph(objectMapper, graph);

        // extract the graph elements
        final ArrayList<Subgraph> sgs = new ArrayList<Subgraph>();
        for (TypeConfig c : action.configs())
        {
            final Subgraph sg = c.extract(document);
            sgs.add(sg);

            if (log.isDebugEnabled())
            {
                final int edges = Iterables.size(sg.edges());
                log.debug("Computing subgraph for /{}/{}, containing {} edges",
                        new Object[]{c.targetIndex(), c.targetType(), edges});
            }
        }

        final Subgraph merged = Subgraphs.merge(sgs);

        if (log.isDebugEnabled())
        {
            final int edges = Iterables.size(merged.edges());
            log.debug("Completed extraction of graph elements, {} subgraphs extracted, total size {} edges", sgs.size(), edges);
        }

        subgraphmanager.commitSubgraph(action.id(), merged);
        log.info("Committed subgraph to graph");

        GraphUtilities.dumpGraph(objectMapper, graph);
    }

    private List<Future<RecomputeResult>> recomputeAffectedDocuments(List<RecomputeRequest> recomputeRequests) throws InterruptedException
    {
        // create Callable from the actions
        // TODO call 'recompute started' for each action to update the status
        final ArrayList<Callable<RecomputeResult>> jobs = new ArrayList<Callable<RecomputeResult>>();
        for (RecomputeRequest r : recomputeRequests)
            jobs.add(recomputeDocument(r));

        // recompute all affected documents and wait for results
        // TODO call 'recompute finished' for each action
        return recomputeQueue.invokeAll(jobs);
    }

    private ArrayList<RecomputeRequest> determineRecomputeActions(DegraphmalizeRequest action)
    {
        final ID id = action.id();

        // we now start traversals for each walk to find documents affected by this change
        final Vertex root = GraphUtilities.findVertex(objectMapper, graph, id);
        if (root == null)
            // TODO this shouldn't occur, because the subgraph implicitly commits a vertex to the graph
            throw new NotFoundInGraphException(id);

        final ArrayList<RecomputeRequest> recomputeRequests = new ArrayList<RecomputeRequest>();

        // we add ourselves (for each config) as the first job(s) in the list
        final VID vid = new VID(objectMapper, root);
        for (TypeConfig c : action.configs())
            recomputeRequests.add(new RecomputeRequest(vid, c));

        // traverse graph in both direction, starting at the root
        log.debug("Computing tree in direction IN, starting at {}", root);
        final Tree<Pair<Edge,Vertex>> up = GraphUtilities.childrenFrom(root, Direction.IN);

        log.debug("Computing tree in direction OUT, starting at {}", root);
        final Tree<Pair<Edge,Vertex>> down = GraphUtilities.childrenFrom(root, Direction.OUT);

        if (log.isDebugEnabled())
        {
            final int up_size = Iterables.size(Trees.bfsWalk(up));
            final int down_size = Iterables.size(Trees.bfsWalk(down));
            log.debug("Found tree of size {} for IN direction and size {} for OUT direction", up_size, down_size);
        }

        // create "dirty document" messages for each node in the tree
        for (Pair<Edge, Vertex> pathElement : Iterables.concat(Trees.bfsWalk(up), Trees.bfsWalk(down)))
        {
            final Vertex v = pathElement.b;

            // skip the root of the tree, ie. ourselves:
            if (v.equals(root))
                continue;

            final VID v_id = new VID(objectMapper, v);

            // we already know this document does not exist in ES, skip
            if (v_id.id().version() == 0)
                continue;

            // alright, mark for computation
            for (TypeConfig c : Configurations.configsFor(cfgProvider.get(), v_id.id().index(), v_id.id().type()))
                recomputeRequests.add(new RecomputeRequest(v_id, c));
        }

        return recomputeRequests;
    }

    /**
     * This procuedure actually performes the recompute of individual documents. It performs transformation, applies walks
     * and inserts/updates the target document.
     *
     * @param action represents the source document and recompute configuration.
     * @return the ES IndexRespons to the insert of the target document.
     */
    private Callable<RecomputeResult> recomputeDocument(final RecomputeRequest action)
    {
        return new Callable<RecomputeResult>()
        {
            @Override
            public RecomputeResult call()
            {
                // TODO pass callback
                final RecomputeCallback cb = new RecomputeCallback()
                {
                };
                return recomputer.recompute(action, cb);
            }
        };
    }

}