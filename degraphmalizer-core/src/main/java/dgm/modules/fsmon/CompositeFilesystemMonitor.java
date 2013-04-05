package dgm.modules.fsmon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compose multiple {@link dgm.configuration.ConfigurationMonitor}s
 *
 */
class CompositeFilesystemMonitor implements FilesystemMonitor
{
    private final List<FilesystemMonitor> monitors = new ArrayList<FilesystemMonitor>();

    public CompositeFilesystemMonitor(FilesystemMonitor... monitors)
    {
        this(Arrays.asList(monitors));
    }

    public CompositeFilesystemMonitor(Iterable<? extends FilesystemMonitor> watchers)
    {
        for(FilesystemMonitor w : watchers)
            this.monitors.add(w);
    }

    @Override
    public final void directoryChanged(String index)
    {
        for(FilesystemMonitor w : monitors)
            w.directoryChanged(index);
    }
}