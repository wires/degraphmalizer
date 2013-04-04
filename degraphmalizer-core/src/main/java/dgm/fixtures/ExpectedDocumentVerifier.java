/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.fixtures;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.inject.Provider;
import dgm.ID;
import dgm.configuration.Configuration;
import dgm.configuration.FixtureIndexConfiguration;
import dgm.configuration.FixtureTypeConfiguration;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * User: rico
 * Date: 03/04/2013
 */
public class ExpectedDocumentVerifier implements PostProcessor
{
    private final Client client;
    private final Provider<Configuration> cfgProvider;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(ExpectedDocumentVerifier.class);

    @Inject
    public ExpectedDocumentVerifier(Client client, Provider<Configuration> cfgProvider, ObjectMapper objectMapper)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run()
    {
        try
        {
            QueryBuilder qb = new MatchAllQueryBuilder();

            String[] indices = Iterables.toArray(cfgProvider.get().getFixtureConfiguration().getIndexNames(), String.class);
            SearchResponse response = client.prepareSearch()
                    .setSearchType(SearchType.QUERY_AND_FETCH)
                    .setNoFields()
                    .setIndices(indices)
                    .setQuery(qb)
                    .setSize(-1)
                    .setVersion(true)
                    .execute().actionGet();


            for (SearchHit hit : response.getHits().getHits())
            {
                ID id = new ID(hit.getIndex(), hit.getType(), hit.getId(), hit.version());
                log.debug("Verifying document:  {}", id);
                if (verifyDocument(id)) {
                    log.info("Document: {} is as expected",id);
                } else {
                    log.warn("Document: {} is not as expected ",id);
                }
            }

        } catch (Exception e)
        {
            log.error("Something went wrong verifying fixture documents.", e);
        }
    }

    private boolean verifyDocument(ID id) throws IOException, ExecutionException, InterruptedException
    {
        FixtureIndexConfiguration expectedIndexConfig = cfgProvider.get().getFixtureConfiguration().getExpectedIndexConfig(id.index());
        if (expectedIndexConfig==null) {
            log.debug("No expected index configuration found for : {}"+id.index());
            return false;
        }
        FixtureTypeConfiguration typeConfig = expectedIndexConfig.getTypeConfig(id.type());
        if (typeConfig==null) {
            log.debug("No expected type configuration found for : {}/{} "+id.index(),id.type());
            return false;
        }
        JsonNode documentExpected = typeConfig.getDocumentById(id.id());
        if (documentExpected!=null) {
            log.debug("No expected document found for : {}/{}/{} "+id.index(),id.type(),id.id());
            return false;
        }
        JsonNode documentInIndex = getDocument(id);
        if (!documentExpected.equals(documentInIndex)) {
            log.debug("Documents are not equal for id {}/{}/{} "+id.index(),id.type(),id.id());
            return false;
        }
        return true;
    }

    private JsonNode getDocument(ID id) throws ExecutionException, InterruptedException, IOException
    {
        final GetResponse response = client.prepareGet(id.index(), id.type(), id.id()).execute().get();

        if (!response.exists())
            return null;

        return objectMapper.readTree(response.getSourceAsString());
    }
}
