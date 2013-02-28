package dgm.configuration;

/**
 * @author Ernst Bunders
 */
public interface FixtureConfiguration
{
    public Iterable<String> getIndexNames();


    public FixtureIndexConfiguration getIndexConfig(String name);

}
