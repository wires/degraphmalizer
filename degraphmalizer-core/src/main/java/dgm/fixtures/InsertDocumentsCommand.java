/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import dgm.ID;
import dgm.configuration.Configuration;
import dgm.configuration.FixtureConfiguration;
import dgm.configuration.FixtureIndexConfiguration;
import dgm.configuration.FixtureTypeConfiguration;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * User: rico
 * Date: 08/04/2013
 */
public class InsertDocumentsCommand implements Command<List<ID>>
{
    protected final Client client;
    protected final Provider<Configuration> cfgProvider;
    protected final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(InsertDocumentsCommand.class);


    @Inject
    public InsertDocumentsCommand(Client client, Provider<Configuration> cfgProvider)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
    }

    @Override
    public List<ID> execute() throws Exception
    {
        List<ID> ids = new ArrayList<ID>();
        FixtureConfiguration fixtureConfig = cfgProvider.get().getFixtureConfiguration();
        // Insert the documents. When this fails, we throw.
        for (String indexName : fixtureConfig.getIndexNames())
        {
            log.debug("Creating index [{}]", indexName);
            try
            {
                FixtureIndexConfiguration indexConfig = fixtureConfig.getIndexConfig(indexName);
                ids.addAll(insertDocuments(indexName, indexConfig));
            } catch (Exception e)
            {
                throw new RuntimeException("something went wrong creating index [" + indexName + "]", e);
            }
        }
        return ids;
    }

    private List<ID> insertDocuments(String indexName, FixtureIndexConfiguration indexConfig) throws UnsupportedEncodingException, ExecutionException, InterruptedException
    {
        List<ID> ids = new ArrayList<ID>();
        for (String typeName : indexConfig.getTypeNames())
        {
            FixtureTypeConfiguration typeConfig = indexConfig.getTypeConfig(typeName);
            if (typeConfig.hasDocuments())
            {
                int c = 0;
                for (String id : typeConfig.getDocumentIds())
                {
                    IndexRequest request = new IndexRequest(indexName, typeName, id)
                        .source(typeConfig.getDocumentById(id).toString().getBytes("UTF-8"))
                        .refresh(true);
                    client.index(request).get();
                    c++;
                    ids.add(new ID(indexName,typeName,id,0));
                }
                log.debug("Inserted {} fixture documents of type [{}] in index [{}]", new Object[]{ c, typeName, indexName});
            }
        }
        return ids;
    }
}
