package degraphmalizr;

import com.google.inject.AbstractModule;

public class DegraphmalizerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(Degraphmalizr.class).to(Degraphmalizer.class).asEagerSingleton();
    }
}
