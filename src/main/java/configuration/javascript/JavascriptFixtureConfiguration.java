 package configuration.javascript;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configuration.FixtureConfiguration;
import configuration.FixtureIndexConfiguration;
import configuration.FixtureTypeConfiguration;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Fixture config contains a number of mappings of index names to index configurations.
 * This class also knows how to a config directory into this object tree.
 * <p/>
 * This is how your file/directory structure should look, for this code to work.
 * The root dir is the 'fixtures' dir in the config directory.
 * <pre>
 *     /[index name]/
 *                   [type name]/
 *                               mapping.json       (type mapping)
 *                               [id].json          (document)
 * </pre>
 * <p/>
 * All json files are parsed and errors are reported with the names of the problematic files, and the cause of the problem.
 *
 * @author Ernst Bunders
 */
public class JavascriptFixtureConfiguration implements FixtureConfiguration
{
    public static final String MAPPING_FILE_NAME = "mapping.json";
    private Map<String, JavascriptFixtureIndexConfiguration> indexConfigs = new LinkedHashMap<String, JavascriptFixtureIndexConfiguration>();
    private static final Logger log = LoggerFactory.getLogger(JavascriptFixtureConfiguration.class);

    public JavascriptFixtureConfiguration(File configDir) throws IOException
    {
        try
        {
            for (File indexDir : configDir.listFiles(FixtureUtil.onlyDirsFilter()))
            {
                indexConfigs.put(indexDir.getName(), new JavascriptFixtureIndexConfiguration(indexDir));
            }
        }
        catch (FixtureConfigurationException e)
        {
            log.error("Could not parse fixture data. Illegal json: " + e.getMessage());
            throw e.getJpe();
        }
    }

    public Iterable<String> getIndexNames()
    {
        return indexConfigs.keySet();
    }

    public FixtureIndexConfiguration getIndexConfig(String name)
    {
        return indexConfigs.get(name);
    }

    @Override
    public String toString()
    {
        return "Fixture configuration. Indexes: " + indexConfigs.toString();
    }
}


/**
 * An index config contains a number of mappings of type names to type configurations.
 */
class JavascriptFixtureIndexConfiguration implements FixtureIndexConfiguration
{
    private Map<String, FixtureTypeConfiguration> typeConfigs = new LinkedHashMap<String, FixtureTypeConfiguration>();

    JavascriptFixtureIndexConfiguration(File configDir) throws IOException
    {
        for (File typeDir : configDir.listFiles(FixtureUtil.onlyDirsFilter()))
            typeConfigs.put(typeDir.getName(), new JavascriptFixtureTypeConfiguration(typeDir));
    }

    public Iterable<String> getTypeNames()
    {
        return typeConfigs.keySet();
    }

    public FixtureTypeConfiguration getTypeConfig(String name)
    {
        return typeConfigs.get(name);
    }

    @Override
    public Iterable<FixtureTypeConfiguration> getTypeConfigurations()
    {
        return (Iterable<FixtureTypeConfiguration>) typeConfigs.values();
    }

    @Override
    public String toString()
    {
        return " index with types:" + typeConfigs.toString();
    }
}


/**
 * The type config contains a number of documents, and
 * optionally an ElasticSearch index mapping for this type.
 */
class JavascriptFixtureTypeConfiguration implements FixtureTypeConfiguration
{
    private final JsonNode mapping;
    private final Map<String, JsonNode> documentsById = new LinkedHashMap<String, JsonNode>();

    JavascriptFixtureTypeConfiguration(File configDir) throws IOException
    {
        mapping = resolveMapping(configDir);
        readDocuments(configDir);
    }

    public JsonNode getMapping()
    {
        return mapping;
    }

    public Iterable<JsonNode> getDocuments(){
        return documentsById.values();
    }

    public Iterable<String>getDocumentIds(){
        return documentsById.keySet();
    }

    public JsonNode getDocumentById(String id)
    {
        return documentsById.get(id);
    }

    @Override
    public boolean hasDocuments()
    {
        return documentsById.size() > 0;
    }

    /**
     * Read all the documents, create JsonNode instances for them, and map them to their id's
     *
     * @param configDir the Type config dir
     * @throws IOException When all else fails.
     */
    private void readDocuments(File configDir) throws IOException
    {
        for (File file : configDir.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return (!JavascriptFixtureConfiguration.MAPPING_FILE_NAME.equals(name)) && name.endsWith(".json");
            }
        }))
        {
            try
            {
                documentsById.put(
                        file.getName().replaceFirst(".json", ""),
                        FixtureUtil.mapper.readTree(FileUtils.readFileToString(file))
                );
            }
            catch (JsonProcessingException e)
            {
                throw new FixtureConfigurationException(e, file);
            }
        }
    }

    /**
     * Reads the mapping for this type, if there is one.
     *
     * @param configDir the Type config dir
     * @return The node created for this document.
     * @throws IOException
     */
    private JsonNode resolveMapping(File configDir) throws IOException
    {
        File mf = new File(configDir, JavascriptFixtureConfiguration.MAPPING_FILE_NAME);
        if (mf.exists() && mf.canRead())
            try
            {
                return FixtureUtil.mapper.readTree(FileUtils.readFileToString(mf));
            } catch (JsonProcessingException e)
            {
                throw new FixtureConfigurationException(e, mf);
            }

        return null;
    }

    @Override
    public String toString()
    {
        return " type that has: " + documentsById.keySet().size() + " documents" + (mapping != null ? " and index mapping" : "");
    }
}

class FixtureConfigurationException extends RuntimeException
{
    private JsonProcessingException jpe;

    FixtureConfigurationException(JsonProcessingException jpe, File cause1)
    {
        super("Could not parse json file [" + cause1.getAbsolutePath() + "] because of:" + jpe.getMessage(), jpe);
        this.jpe = jpe;
    }

    public JsonProcessingException getJpe()
    {
        return jpe;
    }
}

class FixtureUtil
{
    private FixtureUtil() {}

    public static FileFilter onlyDirsFilter()
    {
        return new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                return pathname.isDirectory();
            }
        };
    }

    public static final ObjectMapper mapper = new ObjectMapper();
}
