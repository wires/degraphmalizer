package dgm.fixtures;

import dgm.configuration.ConfigurationMonitor;
import dgm.driver.RunMode;
import dgm.modules.ServiceModule;

/**
 * The module what DegraphmalizeFixturesCommand will be injected into the FixtureLoader.
 * @author Ernst Bunders
 */
public class FixturesModule extends ServiceModule

{
    private final RunMode runMode;

    public FixturesModule(RunMode runMode)
    {
        this.runMode = runMode;
    }

    @Override
    protected void configure()
    {
        switch (runMode)
        {
            case DEVELOPMENT:
                multiBind(ConfigurationMonitor.class).to(FixturesDevelopmentRunner.class);
                bind(FixturesRunner.class).to(FixturesDevelopmentRunner.class);
                break;
            case TEST:
                multiBind(ConfigurationMonitor.class).to(FixturesTestRunner.class);
                bind(FixturesRunner.class).to(FixturesDevelopmentRunner.class);
                break;
            default:
                break;
        }

    }
}
