package dgm.modules.fsmon;

/**
 * Monitor configuration changes
 *
 */
public interface FilesystemMonitor
{
    /**
     * This method is called when the contents of some direction has changed
     */
    void directoryChanged(String directory);
}
