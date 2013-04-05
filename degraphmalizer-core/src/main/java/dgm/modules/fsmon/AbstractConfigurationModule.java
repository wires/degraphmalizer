package dgm.modules.fsmon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import dgm.configuration.Configuration;
import dgm.configuration.javascript.JavascriptConfiguration;
import dgm.exceptions.ConfigurationException;
import dgm.modules.ServiceModule;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Base class for all configuration providers
 */
abstract class AbstractConfigurationModule extends ServiceModule
{
    final String scriptFolder;
    final List<File> libraries;

    AbstractConfigurationModule(String scriptFolder, String... libraries)
    {
        this.scriptFolder = scriptFolder;
        this.libraries = Lists.newArrayList(toFiles(libraries));
    }

    @Override
    protected void configure()
    {
        // bind paths
        bind(String.class).annotatedWith(Names.named("scriptFolder")).toInstance(scriptFolder);
        bind(new TypeLiteral<List<File>>(){}).annotatedWith(Names.named("libraryFiles")).toInstance(libraries);

        configureModule();
    }

    protected abstract void configureModule();

    static Configuration createConfiguration(ObjectMapper om, String scriptFolder, List<File> libraries) throws IOException
    {
        return new JavascriptConfiguration(om, new File(scriptFolder), libraries.toArray(new File[libraries.size()]));
    }

    static File[] toFiles(final String[] filenames)
    {
        final File[] fs = new File[filenames.length];
        int i = 0;
        for(final String fn : filenames)
        {
            final File f = new File(fn);

            if(!f.getName().endsWith(".js"))
                throw new ConfigurationException("Will only load .js files");

            if(!f.canRead())
                throw new ConfigurationException("Cannot read from '" + fn + "'");

            if(!f.isFile())
                throw new ConfigurationException("'" + fn + "' is not a file");

            fs[i] = f;
            i++;
        }

        return fs;
    }
}
