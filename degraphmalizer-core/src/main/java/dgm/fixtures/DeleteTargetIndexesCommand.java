/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import dgm.configuration.*;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: rico
 * Date: 03/04/2013
 */
public class DeleteTargetIndexesCommand implements Command<List<String>>
{
    private final Client client;
    private final Provider<Configuration> cfgProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(WriteResultDocumentsCommand.class);

    @Inject
    public DeleteTargetIndexesCommand(Client client, Provider<Configuration> cfgProvider)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
    }

    @Override
    public List<String> execute() throws Exception
    {
        List<String> indexes = new ArrayList<String>();
        FixtureConfiguration fixtureConfiguration = cfgProvider.get().getFixtureConfiguration();
        Set<String> indexNames = new HashSet<String>();
        for (String index : fixtureConfiguration.getIndexNames())
        {
            for (String type : fixtureConfiguration.getIndexConfig(index).getTypeNames())
            {
                final Iterable<TypeConfig> configs = Configurations.configsFor(cfgProvider.get(), index, type);
                for (TypeConfig typeConfig : configs)
                {
                    indexNames.add(typeConfig.targetIndex());
                }
            }
        }
        for (String indexName : indexNames)
        {
            log.debug("Deleting index [{}]", indexName);
            try
            {
                client.admin().indices().delete(new DeleteIndexRequest(indexName)).get();
                indexes.add(indexName);
            } catch (Exception e)
            {
                log.warn("Something went wrong deleting index [{}], cause: {}. Perhaps it did not exist?", indexName, e.getMessage());
            }
        }
        return indexes;
    }

}
