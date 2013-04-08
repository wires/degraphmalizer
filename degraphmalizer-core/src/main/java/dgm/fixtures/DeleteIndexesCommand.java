/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.fixtures;

import com.google.inject.Provider;
import dgm.configuration.Configuration;
import dgm.configuration.FixtureConfiguration;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * User: rico
 * Date: 08/04/2013
 */
public class DeleteIndexesCommand implements Command<List<String>>
{
    private final Client client;
    private final Provider<Configuration> cfgProvider;

    private static final Logger log = LoggerFactory.getLogger(DeleteIndexesCommand.class);


    @Inject
    public DeleteIndexesCommand(Client client, Provider<Configuration> cfgProvider)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
    }

    @Override
    public List<String> execute() throws Exception
    {
        List<String> indexes = new ArrayList<String>();
        FixtureConfiguration fixtureConfig = cfgProvider.get().getFixtureConfiguration();
        log.debug("Deleting indexes: [{}]", fixtureConfig.getIndexNames());
        for (String indexName : fixtureConfig.getIndexNames())
        {
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
