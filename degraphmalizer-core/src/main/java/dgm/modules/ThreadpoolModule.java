package dgm.modules;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.*;
import dgm.elasticsearch.QueryFunction;
import dgm.modules.bindingannotations.*;

import java.util.concurrent.*;

public class ThreadpoolModule extends AbstractModule
{
    @Override
    protected final void configure()
    {
        bind(QueryFunction.class);
    }

    @Provides
    @Singleton
    @Degraphmalizes
    final ExecutorService provideDegraphmalizesExecutor()
    {
        // single threaded updates!
        final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("degraphmalizer").build();

        return Executors.newSingleThreadExecutor(namedThreadFactory);
    }

    @Provides
    @Singleton
    @Recomputes
    final ExecutorService provideRecomputesExecutor()
    {
        final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("recomputer-%d").build();

        return Executors.newCachedThreadPool(namedThreadFactory);
    }

    @Provides
    @Singleton
    @Fetches
    final ExecutorService provideFetchesExecutor()
    {
        final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("fetcher-%d").build();

        return Executors.newCachedThreadPool(namedThreadFactory);
    }

}