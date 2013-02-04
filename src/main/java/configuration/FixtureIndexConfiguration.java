package configuration;

/**
 * @author Ernst Bunders
 */
public interface FixtureIndexConfiguration
{
    public Iterable<String> getTypeNames();
    public FixtureTypeConfiguration getTypeConfig(String name);
}
