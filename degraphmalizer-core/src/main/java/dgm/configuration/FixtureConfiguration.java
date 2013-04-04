package dgm.configuration;

import java.io.File;

/**
 * @author Ernst Bunders
 */
public interface FixtureConfiguration
{
    public Iterable<String> getIndexNames();
    public FixtureIndexConfiguration getIndexConfig(String name);

    public Iterable<String> getExpectedIndexNames();
    public FixtureIndexConfiguration getExpectedIndexConfig(String name);

    public File getResultsDirectory();
}
