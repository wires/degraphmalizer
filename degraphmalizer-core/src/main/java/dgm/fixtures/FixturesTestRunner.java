/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.fixtures;

import com.google.common.collect.Iterables;
import com.google.inject.Provider;
import dgm.Degraphmalizr;
import dgm.ID;
import dgm.configuration.Configuration;
import dgm.configuration.ConfigurationMonitor;
import dgm.configuration.IndexConfig;
import dgm.configuration.TypeConfig;
import dgm.trees.Pair;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;

/**
 * User: rico
 * Date: 09/04/2013
 */
public class FixturesTestRunner implements ConfigurationMonitor, FixturesRunner
{

    protected final Client client;
    protected final Provider<Configuration> cfgProvider;
    private final Degraphmalizr degraphmalizr;
    RedegraphmalizeCommand redegraphmalizeCommand;
    VerifyResultDocumentsCommand verifyResultDocumentsCommand;

    private static final Logger log = LoggerFactory.getLogger(FixturesDevelopmentRunner.class);

    @Inject
    public FixturesTestRunner(Client client, Provider<Configuration> cfgProvider, Degraphmalizr degraphmalizr)
    {
        this.client = client;
        this.cfgProvider = cfgProvider;
        this.degraphmalizr = degraphmalizr;

        redegraphmalizeCommand = new RedegraphmalizeCommand(client, cfgProvider, degraphmalizr);
        verifyResultDocumentsCommand = new VerifyResultDocumentsCommand(client, cfgProvider);
    }

    public void runFixtures()
    {
        List<ID> ids;
        List<String> indexes;
        List<Pair<ID, Boolean>> verifyResults;

        try
        {

            ids = redegraphmalizeCommand.execute();
            log.info("Degraphmalized {} documents", ids.size());
            verifyResults = verifyResultDocumentsCommand.execute();
            log.info("Checked {} result documents", verifyResults.size());
            int success = 0, failed = 0;
            for (Pair<ID, Boolean> result : verifyResults)
            {
                if (result.b)
                {
                    success++;
                } else
                {
                    failed++;
                }
            }
            log.info("Verify results {} good, {} bad ", success, failed);
        } catch (Exception e)
        {
            log.error("Fixture run failed: {} ", e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void configurationChanged(String change)
    {
        //when the index that was just reloaded is the source index for one of the index configurations, we reinsert the
        //fixtures.
        final Configuration cfg = cfgProvider.get();

        // for quick lookup of the index name
        final HashSet<String> names = new HashSet<String>();
        Iterables.addAll(names, cfg.getFixtureConfiguration().getIndexNames());

        boolean needRun = false;
        for (IndexConfig indexConfig : cfg.indices().values())
            for (TypeConfig typeConfig : indexConfig.types().values())
            {
                if (names.contains(typeConfig.sourceIndex()))
                {
                    needRun = true;
                    break;
                }

            }

        if (needRun)
            try
            {
                runFixtures();
            } catch (Exception e)
            {
                log.error("Something went wrong inserting the fixtures after a configurationChanged event.", e);
            }
    }
}


