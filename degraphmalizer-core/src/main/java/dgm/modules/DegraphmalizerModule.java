package dgm.modules;

import com.google.inject.AbstractModule;
import dgm.Degraphmalizr;
import dgm.degraphmalizr.Degraphmalizer;
import dgm.degraphmalizr.recompute.*;

public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected final void configure()
    {
        bind(Degraphmalizr.class).to(Degraphmalizer.class).asEagerSingleton();
        bind(RecomputeResultFactory.class).to(RRFactory.class).asEagerSingleton();
        bind(Recomputer.class).to(RecomputerFactoryImpl.class).asEagerSingleton();
    }
}
