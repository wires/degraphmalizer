package elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import degraphmalizr.ID;
import graphs.GraphQueries;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.engine.DocumentAlreadyExistsException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.slf4j.Logger;
import trees.Pair;

import java.io.IOException;
import java.util.Iterator;

/**
 *
 */
public class ESUtilities
{
    final protected Logger log;
    final protected Client searchIndex;
    final protected ObjectMapper objectMapper;
    final protected DocumentGetter queryFn;

    @Inject
    public ESUtilities(Logger log, Client searchIndex, ObjectMapper objectMapper)
    {
        this.log = log;
        this.searchIndex = searchIndex;
        this.objectMapper = objectMapper;
        this.queryFn = new DocumentGetter();
    }

    public DocumentGetter documentGetter()
    {
        return queryFn;
    }

    /**
     * Query ElasticSearch for the document
     *
     * TODO caching TODO query priorities
     *
     * TODO optional.absent is now considered a query problem, in the future we want to handle each
     *      case separately, see DGM-45
     *
     * @author wires
     */
    private class DocumentGetter implements Function<Pair<Edge,Vertex>,Optional<GetResponse>>
    {
        public Optional<GetResponse> apply(final Pair<Edge,Vertex> pair)
        {
            // dump information on the current vertex
            if (log.isTraceEnabled())
            {
                log.trace("Retrieving document from ES for vertex {}", pair.b);
                for (String key : pair.b.getPropertyKeys())
                    log.trace("Property {} has value '{}'", key, pair.b.getProperty(key));
            }

            // retrieve id property
            final ID id = GraphQueries.getID(pair.b);

            // vertices without ID's cannot be looked up
            if(id == null)
            {
                log.debug("Vertex has no ID assigned, properties are: ");
                for (String key : pair.b.getPropertyKeys())
                    log.debug("Property {} has value '{}'", key, pair.b.getProperty(key));

                // TODO optional.absent is now considered a query problem, in the future we want to handle each case
                // separately, see DGM-45
                return Optional.absent();
            }

            try
            {
                // query ES for the document
                final GetResponse r = searchIndex.prepareGet(id.index(), id.type(), id.id())
                        .execute().actionGet();

                // query has expired in the meantime
                if (r.version() != id.version())
                {
                    log.debug("Document {} expired, version in ES is {}!", id, r.version());
                    // TODO optional.absent is now considered a query problem, in the future we want to handle each case
                    // separately, see DGM-45
                    return Optional.absent();
                }

                if (!r.exists())
                {
                    log.debug("Document {} does not exist!", id);
                    // TODO optional.absent is now considered a query problem, in the future we want to handle each case
                    // separately, see DGM-45
                    return Optional.absent();
                }

                return Optional.of(r);
            }
            catch (ElasticSearchException e)
            {
                return Optional.absent();
            }
        }
    }


    /**
     * Correctly update a document.
     *
     * <p>
     * The document can be new or existing, in either case, the resulting
     * document has the specified property set to the specified value. Other
     * parts of the document are kept unchanged.
     *
     * <p>
     * This is what this method does:
     *
     * <ul>
     * <li>If object doesn't exist
     * <ul>
     * <li>PUT the object with <code>_create</code>,
     * <li>if this fails, someone has created the object in the mean time
     * </ul>
     * </li>
     * <li>The object exists or PUT/<code>_create</code> has failed
     * <ul>
     * <li>doc, version = GET</li>
     * <li>update doc</li>
     * <li>PUT doc, version</li>
     * <li>if the PUT has failed, retry from GET</li>
     * <li>if the MAX retries was reached, abort</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param index
     *            The ElasticSearch index
     * @param type
     *            The ElasticSearch type
     * @param id
     *            The ID of the document
     * @param property
     *            Which field to update (override) in the document
     * @param value
     *            The field <code>property</code> is set to this field.
     *
     */
    public IndexResponse update(String index, String type, String id, String property, JsonNode value)
    {
        log.trace("Got request to update /{}/{}/{}, set property {} to {}", new Object[]{index, type, id, property, value.toString()});

        // check if the index exists
        final boolean indexExists = searchIndex.admin().indices().prepareExists(index).execute().actionGet().exists();

        log.trace("The index '{}' exists? {}", index, indexExists);

        GetResponse gr = null;

        // do a query if it exists;
        if(indexExists)
        {
            log.trace("About to execute a get request to ES");
            gr = searchIndex.prepareGet(index, type, id).execute().actionGet();
        }

        // object doesn't exist
        if ((!indexExists) || (!gr.exists()))
        {
            log.trace("Document doesn't exist, creating new document");
            try
            {
                // create empty document { .. .. }
                final ObjectNode n = objectMapper.createObjectNode();
                n.put(property, value);

                // put/_create
                final IndexResponse ir = searchIndex.prepareIndex(index, type, id).setSource(n.toString())
                        .setCreate(true).execute().actionGet();

                log.trace("Indexing was succesful, version {}", ir.version());

                return ir;
            }
            catch (DocumentAlreadyExistsException e)
            {
                log.trace("the doc was created after we checked and before we indexed we need to get the document again", e);

                // the doc was created after we checked and before we indexed
                // we need to get the document again
                gr = null;
            }
            catch(Exception e)
            {
                log.trace("Another exception {}", e.getMessage());
                gr = null;
            }
        }

        int retries = 10;
        while (retries > 0)
        {
            // the object exists or was created after we checked and before we
            // indexed
            if (gr == null)
                gr = searchIndex.prepareGet(index, type, id).execute().actionGet();

            log.trace("Found document {} with version {}", id, gr.version());

            // update the document
            JsonNode doc = updateJson(gr.getSourceAsString(), property, value);

            // put again on same version
            try
            {
                IndexResponse ir = searchIndex.prepareIndex(index, type, id).setSource(doc.toString())
                        .setVersion(gr.getVersion()).execute().actionGet();

                return ir;
            }
            catch (VersionConflictEngineException e)
            {
                // someone modified in the mean time, so retry
                log.trace(
                        "Version conflict, somewrote wrote between us getting and indexing the document, {}. Retrying with {} retries left",
                        e, retries);
                // TODO random sleep?
                retries--;
                gr = null;
                continue;
            }
        }

        throw new RuntimeException("Update failed...");
    }

    protected JsonNode updateJson(String doc, String property, JsonNode value)
    {
        // modify the document
        try
        {
            final JsonNode root = objectMapper.readTree(doc);
            return updateJson(root, property, value);
        }
        catch (JsonProcessingException e)
        {
            log.warn("The string '{}' failed to parse as JSON, {}; using empty doc", doc, e);
            return updateJson(objectMapper.createObjectNode(), property, value);
        }
        catch (IOException e)
        {
            log.error(
                    "IO exception while parsing a String? This shouldn't happend but did, {}. using empty doc",
                    e);
            return updateJson(objectMapper.createObjectNode(), property, value);
        }
    }

    protected JsonNode updateJson(JsonNode doc, String property, JsonNode value)
    {
        if (!doc.isObject())
        {
            log.warn("Root json '{}' is not an object! cannot modify", doc.toString());
            return doc;
        }

        final ObjectNode root = objectMapper.createObjectNode();

        final Iterator<String> si = doc.fieldNames();
        while (si.hasNext())
        {
            // skip our property
            final String name = si.next();
            if (name.equals(property))
                continue;

            // copy all other fields
            root.put(name, doc.get(name));
        }

        // append our field
        root.put(property, value);

        return root;
    }
}
