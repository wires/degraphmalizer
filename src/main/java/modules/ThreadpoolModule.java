package modules;

import com.google.inject.*;
import elasticsearch.ESUtilities;
import modules.bindingannotations.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadpoolModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(ESUtilities.class);
    }

    @Provides @Singleton @Degraphmalizes
    ExecutorService provideDegraphmalizesExecutor()
    {
        // single threaded updates!
        return Executors.newSingleThreadExecutor();
    }

    @Provides @Singleton @Recomputes
    ExecutorService provideRecomputesExecutor()
    {
        return Executors.newFixedThreadPool(16);
    }

    @Provides @Singleton @Fetches
    ExecutorService provideFetchesExecutor()
    {
        return Executors.newFixedThreadPool(64);
    }

}