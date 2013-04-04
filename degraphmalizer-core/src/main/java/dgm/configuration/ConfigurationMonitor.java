package dgm.configuration;

/**
 * Monitor configuration changes
 *
 */
public interface ConfigurationMonitor
{
    /**
     * This method is called when the configuration for an index has changed
     */
    void configurationChanged(String index);
}
