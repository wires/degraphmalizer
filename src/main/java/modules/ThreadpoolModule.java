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

    @Provides
    @Singleton
    @Dirty
    ExecutorService provideDirtyExecutor()
    {
        return Executors.newFixedThreadPool(16);
    }

    @Provides
    @Singleton
    @Update
    ExecutorService provideUpdateExecutor()
    {
        // single threaded updates!
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Singleton
    @Fetches
    ExecutorService provideExecutorService()
    {
        return Executors.newFixedThreadPool(64);
    }

}