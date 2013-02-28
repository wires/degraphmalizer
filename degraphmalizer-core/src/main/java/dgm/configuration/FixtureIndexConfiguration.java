package dgm.configuration;

/**
 * @author Ernst Bunders
 */
public interface FixtureIndexConfiguration
{
    public Iterable<String> getTypeNames();
    public FixtureTypeConfiguration getTypeConfig(String name);
    public Iterable<FixtureTypeConfiguration> getTypeConfigurations();
}
