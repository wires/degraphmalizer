package dgm.configuration;

import java.util.*;

/**
 * Compose multiple {@link ConfigurationMonitor}s
 *
 */
public class CompositeConfigurationMonitor implements ConfigurationMonitor
{
    private final List<ConfigurationMonitor> monitors = new ArrayList<ConfigurationMonitor>();

    public CompositeConfigurationMonitor(ConfigurationMonitor... monitors)
    {
        this(Arrays.asList(monitors));
    }

    public CompositeConfigurationMonitor(Iterable<? extends ConfigurationMonitor> watchers)
    {
        for(ConfigurationMonitor w : watchers)
            this.monitors.add(w);
    }

    @Override
    public final void configurationChanged(String index)
    {
        for(ConfigurationMonitor w : monitors)
            w.configurationChanged(index);
    }
}