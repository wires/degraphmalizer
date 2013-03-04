package dgm.degraphmalizr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.tinkerpop.blueprints.*;
import dgm.*;
import dgm.configuration.*;
import dgm.degraphmalizr.degraphmalize.*;
import dgm.degraphmalizr.recompute.*;
import dgm.exceptions.*;
import dgm.graphs.Subgraphs;
import dgm.modules.bindingannotations.*;
import dgm.modules.elasticsearch.QueryFunction;
import dgm.trees.*;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

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
    public final DegraphmalizeAction degraphmalize(DegraphmalizeActionType actionType, ID id, DegraphmalizeStatus callback)
    {
        // find all matching configurations
        final Iterable<TypeConfig> configs = Configurations.configsFor(cfgProvider.get(), id.index(), id.type());

        // we cannot handle this request!
        if(Iterables.isEmpty(configs))
            throw new NoConfiguration(id);

        // construct the action object
        final DegraphmalizeAction action = new DegraphmalizeAction(actionType, id, configs, callback);

        // convert object into task and queue
        action.result = degraphmalizeQueue.submit(degraphmalizeJob(action));

        return action;
    }

    private Callable<JsonNode> degraphmalizeJob(final DegraphmalizeAction action)
	{
		return new Callable<JsonNode>()
		{
			@Override
			public JsonNode call() throws Exception
			{
                final DegraphmalizeActionType actionType = action.type();

                switch(actionType)
                {
                    case UPDATE:
                        return doUpdate(action);

                    case DELETE:
                        return doDelete(action);
                }

                throw new UnreachableCodeReachedException();
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

    private List<RecomputeRequest> determineRecomputeActionsOrEmpty(DegraphmalizeAction action)
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

    private JsonNode doUpdate(DegraphmalizeAction action) throws Exception
    {
        try
        {
            log.info("Processing request for id {}", action.id());

            // Get document from elasticsearch
            final JsonNode jsonNode = getDocument(action.id());

            // couldn't find source document, so we are done
            if (jsonNode == null)
            {
                // TODO cleanup status/result/action objects.
                action.status().complete(DegraphmalizeResult.success(action));
                return objectMapper.createObjectNode().put("success",true);
            }
            else
            {
                // find all document connected to this document before changing the graph
                final List<RecomputeRequest> pre = determineRecomputeActionsOrEmpty(action);

                action.setDocument(jsonNode);

                // update the graph
                generateSubgraph(action);

                final List<RecomputeRequest> post = determineRecomputeActions(action);

                // add all the missing requests from pre to post
                for(RecomputeRequest r : pre)
                    if(!inList(r, post))
                        post.add(r);

                logRecomputes(action.id(), post);
                final List<Future<RecomputeResult>> results = recomputeAffectedDocuments(post);

                // TODO refactor action class
                action.status().complete(DegraphmalizeResult.success(action));

                return getStatusJsonNode(results);
            }
        }
        catch (final DegraphmalizerException e)
        {
            final DegraphmalizeResult result = DegraphmalizeResult.failure(action, e);

            // report failure
            action.status().exception(result);

            // rethrow, this will captured by Future<>.get()
            throw e;
        }
        catch (final Exception e)
        {
            final DegraphmalizeResult result = DegraphmalizeResult.failure(action, new WrappedException(e));

            // report failure
            action.status().exception(result);

            // rethrow, this will captured by Future<>.get()
            throw e;
        }
    }

    private void logRecomputes(ID id, List<RecomputeRequest> recomputeRequests)
    {
        // fn = toString . id . root
        final Function<RecomputeRequest,String> fn = new Function<RecomputeRequest, String>() {
            @Override public String apply(@Nullable RecomputeRequest input) {
                return "\n\t" + input.root.id().toString();
            }
        };

        // intercalate "," $ map fn recomputeRequests
        final String ids = Joiner.on("").join(Lists.transform(recomputeRequests, fn));

        log.info("Degraphmalize request for {} triggered compute of: {}", id, ids);
    }

    // TODO refactor (dubbeling met doUpdate) (DGM-44)
    private JsonNode doDelete(DegraphmalizeAction action) throws Exception
    {
        try
        {
            List<RecomputeRequest> recomputeRequests = determineRecomputeActions(action);

            subgraphmanager.deleteSubgraph(action.id());

            // TODO refactor action class
            action.status().complete(DegraphmalizeResult.success(action));

            logRecomputes(action.id(), recomputeRequests);
            final List<Future<RecomputeResult>> results = recomputeAffectedDocuments(recomputeRequests);

            return getStatusJsonNode(results);
        }
        catch (final NotFoundInGraphException e)
        {
            // TODO move to proper place
            log.warn("While doing delete, could not find central node for ID {}", e.id());
            action.status().complete(DegraphmalizeResult.success(action));
            return objectMapper.createObjectNode().put("jelle", "was here");
        }
        catch (final DegraphmalizerException e)
        {
            final DegraphmalizeResult result = DegraphmalizeResult.failure(action, e);

            // report failure
            action.status().exception(result);

            // rethrow, this will captured by Future<>.get()
            throw e;
        }
        catch (final Exception e)
        {
            final DegraphmalizeResult result = DegraphmalizeResult.failure(action, new WrappedException(e));

            // report failure
            action.status().exception(result);

            // rethrow, this will captured by Future<>.get()
            throw e;
        }
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

    private void generateSubgraph(DegraphmalizeAction action) {
        final ID id = action.id();

        GraphUtilities.dumpGraph(objectMapper, graph);

        // extract the graph elements
        final ArrayList<Subgraph> sgs = new ArrayList<Subgraph>();
        for(TypeConfig c : action.configs())
        {
            final Subgraph sg = c.extract(action.document());
            sgs.add(sg);

            if(log.isDebugEnabled())
            {
                final int edges = Iterables.size(sg.edges());
                log.debug("Computing subgraph for /{}/{}, containing {} edges",
                        new Object[]{c.targetIndex(), c.targetType(), edges});
            }
        }

        final Subgraph merged = Subgraphs.merge(sgs);

        if(log.isDebugEnabled())
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
        for(RecomputeRequest r : recomputeRequests)
            jobs.add(recomputeDocument(r));

        // recompute all affected documents and wait for results
        // TODO call 'recompute finished' for each action
        return recomputeQueue.invokeAll(jobs);
    }

    private ArrayList<RecomputeRequest> determineRecomputeActions(DegraphmalizeAction action)
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
        for(TypeConfig c : action.configs())
            recomputeRequests.add(new RecomputeRequest(vid, c));

        // traverse graph in both direction, starting at the root
        log.debug("Computing tree in direction IN, starting at {}", root);
        final Tree<Pair<Edge,Vertex>> up = GraphUtilities.childrenFrom(graph, root, Direction.IN);

        log.debug("Computing tree in direction OUT, starting at {}", root);
        final Tree<Pair<Edge,Vertex>> down = GraphUtilities.childrenFrom(graph, root, Direction.OUT);

        if(log.isDebugEnabled())
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
            if(v.equals(root))
                continue;

            final VID v_id = new VID(objectMapper, v);

            // we already know this document does not exist in ES, skip
            if(v_id.id().version() == 0)
                continue;

            // alright, mark for computation
            for(TypeConfig c : Configurations.configsFor(cfgProvider.get(), v_id.id().index(), v_id.id().type()))
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
                final RecomputeCallback cb = new RecomputeCallback(){};
                return recomputer.recompute(action, cb);
            }
        };
    }

    // TODO deprecate, also kill the method in JSONUtilities
    private ObjectNode getStatusJsonNode(List<Future<RecomputeResult>> results) throws InterruptedException, ExecutionException
    {
        final RecomputeResult ourResult = results.get(0).get();
        return toJSON(objectMapper, ourResult);
    }

    public static ObjectNode toJSON(ObjectMapper objectMapper, RecomputeResult success) throws InterruptedException, ExecutionException
    {
        final ObjectNode n = objectMapper.createObjectNode();

        n.put("success", true);

        // write targetID using index reponse
        final IndexResponse ir = success.indexResponse();
        final ObjectNode targetID = objectMapper.createObjectNode();
        targetID.put("index", ir.index());
        targetID.put("type", ir.type());
        targetID.put("id", ir.id());
        targetID.put("version", ir.version());
        n.put("targetID", targetID);

        // write dictionary of properties and their values
        final ObjectNode properties = objectMapper.createObjectNode();
        for(Map.Entry<String,JsonNode> entry : success.properties().entrySet())
            properties.put(entry.getKey(), entry.getValue());
        n.put("properties", properties);

        // write dictionary of properties and their values
        n.put("sourceDocumentAfterTransform", success.sourceDocument());

        return n;
   }

}