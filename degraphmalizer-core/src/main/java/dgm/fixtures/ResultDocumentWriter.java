/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.fixtures;

import com.google.inject.Provider;
import dgm.ID;
import dgm.configuration.Configuration;
import dgm.configuration.Configurations;
import dgm.configuration.FixtureConfiguration;
import dgm.configuration.TypeConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

/**
 * User: rico
 * Date: 03/04/2013
 */
public class ResultDocumentWriter implements PostProcessor
{
    private final Client client;
    private final Provider<Configuration> cfgProvider;

    private static final Logger log = LoggerFactory.getLogger(ResultDocumentWriter.class);

    @Inject
    public ResultDocumentWriter(Client client, Provider<Configuration> cfgProvider)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
    }

    @Override
    public void run()
    {
        FixtureConfiguration fixtureConfiguration = cfgProvider.get().getFixtureConfiguration();
        for (String index : fixtureConfiguration.getIndexNames())
        {
            for (String type : fixtureConfiguration.getIndexConfig(index).getTypeNames())
            {
                final Iterable<TypeConfig> configs = Configurations.configsFor(cfgProvider.get(), index, type);
                for (TypeConfig typeConfig : configs)
                {
                    writeDocuments(typeConfig.targetIndex(), typeConfig.targetType());
                }
            }
        }
    }

    private void writeDocuments(String index, String type)
    {
        try
        {
            File resultsDirectory = cfgProvider.get().getFixtureConfiguration().getResultsDirectory();
            if (!resultsDirectory.exists())
            {
                resultsDirectory.mkdir();
            }
            if (!resultsDirectory.isDirectory())
            {
                log.error("Results directory is not a directory");
                throw new RuntimeException("Results directory is not a directory");
            }

            QueryBuilder qb = new MatchAllQueryBuilder();

            SearchResponse response = client.prepareSearch()
                    .setSearchType(SearchType.QUERY_AND_FETCH)
                    .setNoFields()
                    .setIndices(index)
                    .setTypes(type)
                    .setQuery(qb)
                    .setSize(-1)
                    .setVersion(true)
                    .execute().actionGet();


            for (SearchHit hit : response.getHits().getHits())
            {
                ID id = new ID(hit.getIndex(), hit.getType(), hit.getId(), hit.version());
                log.debug("Writing document:  {}", id);
                writeDocument(resultsDirectory, id);
            }

        } catch (Exception e)
        {
            log.error("Something went wrong writing fixture result documents.", e);
        }
    }

    private void writeDocument(File directory, ID id) throws IOException, ExecutionException, InterruptedException
    {
        String document = getDocument(id);
        if (StringUtils.isNotEmpty(document))
        {
            File dir = new File(directory, id.index() + File.separator + id.type());
            if (!dir.exists())
            {
                dir.mkdirs();
            }
            FileUtils.writeStringToFile(new File(dir, id.id()), document, Charset.forName("UTF-8"));
        }
    }

    private String getDocument(ID id) throws ExecutionException, InterruptedException, IOException
    {
        final GetResponse response = client.prepareGet(id.index(), id.type(), id.id()).execute().get();

        if (!response.exists())
            return null;

        return response.getSourceAsString();
    }
}
