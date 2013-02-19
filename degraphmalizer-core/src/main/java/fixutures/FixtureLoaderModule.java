package fixutures;

import com.google.inject.AbstractModule;
import configuration.FixtureConfiguration;
import driver.RunMode;

/**
 * The module what PostProcessor will be injected into the FixtureLoader.
 * @author Ernst Bunders
 */
public class FixtureLoaderModule extends AbstractModule

{
    private final RunMode runMode;

    public FixtureLoaderModule(RunMode runMode)
    {
        this.runMode = runMode;
    }

    @Override
    protected void configure()
    {
        switch (runMode)
        {
            case DEVELOPMENT:
                bind(PostProcessor.class).to(RedegraphmalizePostProcessor.class);
                break;
            default:
                bind(PostProcessor.class).to(DummyPostProcessor.class);
        }

    }
}
