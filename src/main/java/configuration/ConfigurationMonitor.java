package configuration;

/**
 * Monitor configuration changes
 *
 */
public interface ConfigurationMonitor
{
    /**
     * This method is called when the configuration for an index has changed
     *
     * @param index
     */
    void configurationChanged(String index);
}
