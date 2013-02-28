package dgm.configuration;

import java.util.*;

/**
 * Compose multiple {@link ConfigurationMonitor}s
 *
 */
public class CompositeMonitor implements ConfigurationMonitor
{
    private final List<ConfigurationMonitor> monitors = new ArrayList<ConfigurationMonitor>();

    public CompositeMonitor(ConfigurationMonitor... monitors)
    {
        this(Arrays.asList(monitors));
    }

    public CompositeMonitor(Iterable<? extends ConfigurationMonitor> watchers)
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