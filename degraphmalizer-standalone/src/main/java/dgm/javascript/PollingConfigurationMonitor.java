package dgm.configuration.javascript;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import dgm.configuration.ConfigurationMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Monitor the configuration directory for changes in the index configurations.
 */
public class PollingConfigurationMonitor implements Runnable
{
    protected final ConfigurationMonitor monitor;
    protected final File directory;
    protected final int interval;

    protected final Thread poller = new Thread(this);

    public PollingConfigurationMonitor(String directory, int interval, ConfigurationMonitor monitor)
    {
        this.directory = new File(directory);
        this.interval = interval;
        this.monitor = monitor;

        if(interval < 100)
            throw new RuntimeException("Don't monitor faster than once every 100ms");
    }

    public final void start()
    {
        if(poller.isAlive())
            return;

        poller.start();
    }

    @Override
    public final void run()
    {
        Map<String, HashCode> state = null;

        while(true)
        {
            try
            {
                Thread.sleep(interval);
            }
            catch (InterruptedException e)
            {
                break;
            }

            // compute the state of each index
            final Map<String, HashCode> newState = new HashMap<String, HashCode>();
            for(File d : nullSafeFileList(directory.listFiles()))
            {
                // skip non directories
                if(!d.isDirectory())
                    continue;

                // skip empty directories
                if(nullSafeFileList(d.listFiles()).length == 0)
                    continue;

                // each subdirectory encodes an index, so compute it's state hash
                newState.put(d.getName(), subdirState(d));
            }

            // first scan, assign state and ignore
            if(state == null)
            {
                state = newState;
                continue;
            }

            // find changed indices
            final MapDifference<String, HashCode> diff = Maps.difference(state, newState);

            // update state
            state = newState;

            // nothing has changed, so sleep again
            if(diff.areEqual())
                continue;

            // directory contents have changed
            for(Map.Entry<String,MapDifference.ValueDifference<HashCode>> index : diff.entriesDiffering().entrySet())
                monitor.configurationChanged(index.getKey());

            // directory added
            for(Map.Entry<String, HashCode> index : diff.entriesOnlyOnRight().entrySet())
                monitor.configurationChanged(index.getKey());

            // directory removed
            for(Map.Entry<String, HashCode> index : diff.entriesOnlyOnLeft().entrySet())
                monitor.configurationChanged(index.getKey());
        }
    }

    File[] nullSafeFileList(File[] input){
        return input != null ? input : new File[] {};
    }


    /**
     * The hash is computed based on the modification times, file size and file names of all files recursively.
     * The order in which the files are listed doesn't matter.
     *
     * If the returned hash changed between calls, this means that something in the subdirectory has changed.
     *
     * @return a hashcode representing the subdirectory state.
     */
    protected static HashCode subdirState(File directory)
    {
        // MD5 hash some attributes of each file
        final HashFunction hf = Hashing.md5();
        final ArrayList<HashCode> codes = new ArrayList<HashCode>(1000);

        // non recursively load all configuration files
        final WildcardFileFilter filter = new WildcardFileFilter(new String[]{"*.conf.js", "*.json"});
        final Iterator<File> fi = FileUtils.iterateFiles(directory, filter, null);
        while(fi.hasNext())
        {
            final File file = fi.next();

            // add some attributes to the hasher
            final Hasher hasher = hf.newHasher();
            hasher.putString(file.getAbsolutePath());
            hasher.putLong(file.lastModified());
            hasher.putLong(file.length());

            // we collect all hash codes...
            codes.add(hasher.hash());
        }

        // when we have no files, return some fixed hash
        if(codes.size() == 0)
        {
            final Hasher hasher = hf.newHasher();
            hasher.putLong(0);
            return hasher.hash();
        }

        // ... because we don't care about order in which files are listed
        return Hashing.combineUnordered(codes);
    }
}