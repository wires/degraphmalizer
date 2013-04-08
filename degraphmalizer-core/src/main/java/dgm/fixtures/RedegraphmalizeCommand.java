package dgm.fixtures;

import com.google.common.collect.Iterables;
import com.google.inject.Provider;
import dgm.Degraphmalizr;
import dgm.ID;
import dgm.configuration.Configuration;
import dgm.configuration.Configurations;
import dgm.configuration.FixtureConfiguration;
import dgm.configuration.TypeConfig;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeRequestScope;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeRequestType;
import dgm.degraphmalizr.degraphmalize.DegraphmalizeResult;
import dgm.degraphmalizr.degraphmalize.LoggingDegraphmalizeCallback;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This thing runs after fixture data has been inserted.
 * The purpose is to degraphmalize every document that has just been inserted, so all documents
 * in each index for which there is a fixture configuration.
 *
 * @author Ernst Bunders
 */
public class RedegraphmalizeCommand implements Command<List<ID>>
{
    private final Client client;
    private final Provider<Configuration> cfgProvider;
    private final Degraphmalizr degraphmalizr;

    private static final Logger log = LoggerFactory.getLogger(RedegraphmalizeCommand.class);

    @Inject
    public RedegraphmalizeCommand(Client client, Provider<Configuration> cfgProvider, Degraphmalizr degraphmalizr)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
        this.degraphmalizr = degraphmalizr;
    }

    @Override
    public List<ID> execute()
    {
        List<ID> ids = new ArrayList<ID>();
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
                log.debug("Re-degraphmalizing document {}", id);
                Future<DegraphmalizeResult> futureResult = degraphmalizr.degraphmalize(DegraphmalizeRequestType.UPDATE, DegraphmalizeRequestScope.DOCUMENT, id, new LoggingDegraphmalizeCallback());
                try
                {
                    DegraphmalizeResult result = futureResult.get();
                    log.debug("Re-degraphmalized document {}", result.root());
                    ids.add(id);
                } catch (ExecutionException ee)
                {
                    log.warn("Degraphmalize not successful {} ", ee);
                }
            }

            log.info("Flushing target indexes");
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
            RefreshRequestBuilder builder = client.admin().indices().prepareRefresh(indexNames.toArray(new String[indexNames.size()]));
            RefreshResponse refreshResponse = builder.execute().get();
            log.info("Target indexes flushed {}", indexNames);
        } catch (Exception e)
        {
            log.error("Something went wrong re-degraphmalizing fixture documents.", e);
        }
        return ids;
    }

}
