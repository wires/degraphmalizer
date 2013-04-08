/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Provider;
import dgm.configuration.Configuration;
import dgm.configuration.FixtureConfiguration;
import dgm.configuration.FixtureIndexConfiguration;
import dgm.configuration.FixtureTypeConfiguration;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: rico
 * Date: 08/04/2013
 */
public class CreateIndexesCommand implements Command<List<String>>
{
    private final Client client;
    private final Provider<Configuration> cfgProvider;
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(CreateIndexesCommand.class);

    @Inject
    public CreateIndexesCommand(Client client, Provider<Configuration> cfgProvider)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
    }

    @Override
    public List<String> execute() throws Exception
    {
        List<String> indexes = new ArrayList<String>();
        FixtureConfiguration fixtureConfig = cfgProvider.get().getFixtureConfiguration();
        for (String indexName : fixtureConfig.getIndexNames())
        {
            log.debug("Creating index [{}]", indexName);
            FixtureIndexConfiguration indexConfig = fixtureConfig.getIndexConfig(indexName);
            try
            {
                client.admin().indices().create(buildCreateIndexRequest(indexName, indexConfig)).get();
                indexes.add(indexName);
            } catch (Exception e)
            {
                throw new Exception("something went wrong creating index [" + indexName + "]", e);
            }
        }
        return indexes;
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

    private Settings createSettings()
    {
        ObjectNode indexConfigSettingsNode = objectMapper.createObjectNode();
        indexConfigSettingsNode.
                put("number_of_shards", 2).
                put("number_of_replicas", 1);

        return ImmutableSettings.settingsBuilder().loadFromSource(indexConfigSettingsNode.toString()).build();
    }
}
