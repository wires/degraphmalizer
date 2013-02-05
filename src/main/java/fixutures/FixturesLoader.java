package fixutures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import com.google.inject.Provider;
import configuration.Configuration;
import configuration.FixtureConfiguration;
import configuration.FixtureIndexConfiguration;
import configuration.FixtureTypeConfiguration;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;

/**
 * This class loads fixture data into elasticsearch. The procedure is as follows:
 * For each index you want to bootstrap fixture data into:
 * - remove the index.
 * - gather index mapping for al types you have for the index.
 * - build an index configuration with composite type configuration (use default index shard/backup values for testing purposes)
 * - create the index
 * - insert the documents.
 *
 * @author Ernst Bunders
 */
public class FixturesLoader
{
    protected final Client client;
    protected final Provider<Configuration> cfgProvider;
    protected final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(FixturesLoader.class);

    @Inject
    public FixturesLoader(Client client, Provider<Configuration> cfgProvider)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
    }

    /**
     *
     * @throws Exception When the creation of an index or the inserting of a document fails.
     */
    public void loadFixtures() throws Exception
    {
        FixtureConfiguration fixtureConfig = cfgProvider.get().getFixtureConfiguration();
        if (fixtureConfig == null){
            log.info("No fixture config found. No fixtures loaded.");
            return;
        }
        log.debug("Start processing fixtures");

        //first delete all the indexes we want to insert fixtures into. When this fails, we go on, assuming the index didn't exist.
        log.debug("Deleting indexes: [{}]", fixtureConfig.getIndexNames());
        for(String indexName: fixtureConfig.getIndexNames()){
            try
            {
                client.admin().indices().delete(new DeleteIndexRequest(indexName)).get();

            } catch (Exception e)
            {
                log.warn("Something went wrong deleting index [{}], cause: {}. Perhaps it did not exist?", indexName, e.getMessage());
            }
        }

        //now create the indexes and insert the documents. When this fails, we throw.
        for (String indexName : fixtureConfig.getIndexNames())
        {
            FixtureIndexConfiguration indexConfig = fixtureConfig.getIndexConfig(indexName);
            log.debug("Creating index [{}]", indexName);
            try
            {
                client.admin().indices().create(buildCreateIndexRequest(indexName, indexConfig)).get();
                insertDocuments(indexName, indexConfig);
            } catch (Exception e)
            {
                throw new Exception("something went wrong creating index [" + indexName + "]", e);
            }
        }
    }

    private void insertDocuments(String indexName, FixtureIndexConfiguration indexConfig) throws UnsupportedEncodingException, ExecutionException, InterruptedException
    {
        for (String typeName : indexConfig.getTypeNames())
        {
            FixtureTypeConfiguration typeConfig = indexConfig.getTypeConfig(typeName);
            if (typeConfig.hasDocuments())
            {
                int c = 0;
                for (String id : typeConfig.getDocumentIds())
                {
                    IndexRequest request = new IndexRequest(indexName, typeName, id);
                    request.source(typeConfig.getDocumentById(id).toString().getBytes("UTF-8"));
                    client.index(request).get();
                    c++;
                }
                log.debug("Inserted {} fixture documents of type [{}] in index [{}]", new Object[]{ c, typeName, indexName});
            }
        }
    }

    private Settings createSettings()
    {
        ObjectNode indexConfigSettingsNode = mapper.createObjectNode();
        indexConfigSettingsNode.
                put("number_of_shards", 2).
                put("number_of_replicas", 1);

        Settings settings = ImmutableSettings.settingsBuilder().loadFromSource(indexConfigSettingsNode.toString()).build();
        return settings;
    }

    CreateIndexRequest buildCreateIndexRequest(String indexName, FixtureIndexConfiguration indexConfig) throws IOException
    {
        CreateIndexRequest request = new CreateIndexRequest(indexName, createSettings());
        for (String typeName : indexConfig.getTypeNames())
        {
            FixtureTypeConfiguration typeConfig = indexConfig.getTypeConfig(typeName);
            if (typeConfig.getMapping() != null)
            {
                request.mapping(typeName, typeConfig.getMapping().toString());
                log.debug("Add mapping for type [{}] in index [{}]", typeName, indexName);
            }
        }
        return request;
    }
}
