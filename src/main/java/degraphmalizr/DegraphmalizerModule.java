package degraphmalizr;

import com.google.inject.AbstractModule;

public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected final void configure()
    {
        bind(Degraphmalizr.class).to(Degraphmalizer.class).asEagerSingleton();
    }
}
