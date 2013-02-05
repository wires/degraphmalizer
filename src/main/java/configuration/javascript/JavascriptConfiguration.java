package configuration.javascript;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import configuration.*;
import degraphmalizr.jobs.DegraphmalizeAction;
import elasticsearch.ResolvedPathElement;
import graphs.ops.CompositeSubgraph;
import graphs.ops.LoggingSubgraph;
import graphs.ops.Subgraph;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.elasticsearch.action.get.GetResponse;
import org.mozilla.javascript.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trees.Tree;
import trees.Trees;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Load configuration from javascript files in a directory
 */
public class JavascriptConfiguration implements Configuration
{
    public static final String FIXTURES_DIR_NAME = "fixtures";

    private final Map<String,JavascriptIndexConfig> indices = new HashMap<String,JavascriptIndexConfig>();
    private JavascriptFixtureConfiguration fixtureConfig;

    private static final Logger log = LoggerFactory.getLogger(JavascriptConfiguration.class);



    public JavascriptConfiguration(File directory) throws IOException
    {
        //todo: directory.listFiles() can return null
        for(File dir : directory.listFiles())
        {
            // skip non directories
            if(!dir.isDirectory())
                continue;

            // each subdirectory encodes an index
            final String dirname = dir.getName();
            if (FIXTURES_DIR_NAME.equals(dirname)) {
                fixtureConfig = new JavascriptFixtureConfiguration(dir);
                log.debug(fixtureConfig.toString());
            }else{
                indices.put(dirname, new JavascriptIndexConfig(dirname, dir));
            }
        }
    }

    @Override
    public Map<String, ? extends IndexConfig> indices()
    {
        return indices;
    }

    @Override
    public FixtureConfiguration getFixtureConfiguration()
    {
        return fixtureConfig;
    }
}


class JavascriptIndexConfig implements IndexConfig
{
    private static final Logger log = LoggerFactory.getLogger(JavascriptIndexConfig.class);

	final String index;
	final Scriptable scope;
	final Map<String,JavascriptTypeConfig> types = new HashMap<String,JavascriptTypeConfig>();
	
	
	/**
	 * Scan, filter and watch a directory for correct javascript files.
	 *
     * @param index The elastic search index to write to
	 * @param directory Directory to watch for files
	 * @throws IOException
	 */
	public JavascriptIndexConfig(String index, File directory) throws IOException {

        this.index = index;
        Scriptable scope = null;

        try {
            final Context cx = Context.enter();

            // create standard ECMA scope
            scope = cx.initStandardObjects();

            // load underscore and the Subgraph class
            loadLib(cx, scope, "underscore-1.4.0.js");
            loadLib(cx, scope, "subgraph.js");

            // close the root scope for modifications
            cx.seal(scope);

            // non recursively load all configuration files
            final FilenameFilter filenameFilter = new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    if(name.endsWith(".conf.js"))
                        return true;
                    log.error("File [{}] in config dir [{}] has wrong name format and is ignored. Proper format: [target type].conf.js", name, dir.getAbsolutePath());
                    return false;
                }
            };

            for(File file: directory.listFiles(filenameFilter))
            {
                log.debug("Found config file [{}] for index [{}]", file.getCanonicalFile(), index);
                final Reader reader = new FileReader(file);
                final String fn = file.getCanonicalPath();
                final String type = file.getName().replaceFirst(".conf.js", "");

                final Scriptable typeConfig = (Scriptable) compile(cx, scope, reader, fn);

                types.put(type, new JavascriptTypeConfig(type, scope, typeConfig, this));
            }
        } finally {
            Context.exit();
        }

        if (scope == null)
            throw new RuntimeException("Scope failed to initialize");

        this.scope = scope;
    }


    @Override
    public String name()
    {
	    return index;
    }

	@Override
    public Map<String,? extends TypeConfig> types()
    {
	    return types;
    }
	
	private Object compile(Context cx, Scriptable scope, Reader reader, String fn) throws IOException
	{
		// compile and execute into the scope
		return cx.compileReader(reader, fn, 0, null).exec(cx, scope);
	}
	
	private Object loadLib(Context cx, Scriptable scope, String fn) throws IOException
	{
		// get loader relative to this class
		final Reader reader = new InputStreamReader(JavascriptIndexConfig.class.getResourceAsStream(fn), "UTF-8");
		
		return compile(cx, scope, reader, fn);
	}
}

class JavascriptTypeConfig implements TypeConfig
{
    private static final Logger log = LoggerFactory.getLogger(JavascriptTypeConfig.class);
    final IndexConfig indexConfig;
	final String type;
	final Scriptable script;

	final Function filter;
	final Function extract;
	final Function transform;

    final String sourceIndex;
    final String sourceType;


    final Map<String,WalkConfig> walks = new HashMap<String, WalkConfig>();

    public JavascriptTypeConfig(String type, Scriptable scope, Scriptable script, IndexConfig indexConfig) throws IOException
	{
		this.type = type;
		this.script = script;
		this.indexConfig = indexConfig;

        log.debug("Creating config for type [{}] in index [{}]", type, indexConfig.name());

        try{
		    Context.enter();

            // filter & graph extraction functions
            filter = (Function) fetchObjectOrNull("filter");
            extract = (Function) fetchObjectOrNull("extract");
            transform = (Function) fetchObjectOrNull("transform");

            //TODO: null check, invalid configuration, error handling
            sourceIndex = ScriptableObject.getTypedProperty(script, "sourceIndex", String.class);
            sourceType = ScriptableObject.getTypedProperty(script, "sourceType", String.class);

            // add the walks
            final Scriptable walks = (Scriptable) fetchObjectOrNull("walks");
            if (walks != null) {
                for(Object id : ScriptableObject.getPropertyIds(walks))
                {
                    final String walkName = id.toString();
        
                    // get the walk object
                    final Scriptable walk = (Scriptable)ScriptableObject.getProperty(walks, walkName);
        
                    final Direction direction = Direction.valueOf(ScriptableObject.getProperty(walk, "direction").toString());
        
                    final Scriptable properties = (Scriptable)ScriptableObject.getProperty(walk, "properties");
        
                    final JavascriptWalkConfig walkCfg = new JavascriptWalkConfig(walkName, direction, this, scope, properties);
        
                    this.walks.put(walkName, walkCfg);
                }
            }else{
                log.debug("No walks found in configuration");
            }
        }
        finally
        {

            Context.exit();
        }
	}

    private Object fetchObjectOrNull(String field)
    {
        final Object obj = ScriptableObject.getProperty(script, field);

        // field not specified in script
        if(obj == UniqueTag.NOT_FOUND)
            return null;

        return obj;
    }


	@Override
    public String name()
    {
	    return type;
    }

	@Override
    public void extract(DegraphmalizeAction job, final Subgraph graphops)
    {
        if (extract == null)
        {
            log.debug("Not extracting subgraph because no extract() function is configured");
            return;
        }

        final Context cx = Context.enter();

        // extract graph components
        final LoggingSubgraph lsg = new LoggingSubgraph("subgraph");
        final CompositeSubgraph csg = new CompositeSubgraph(lsg, graphops);
        final JavascriptSubgraph sg = new JavascriptSubgraph(csg, cx, script);
        final Object obj = JSONUtilities.toJSONObject(cx, script,  job.document().toString());
        extract.call(cx, script, null, new Object[]{obj, sg});

        Context.exit();

    }

	@Override
    public boolean filter(JsonNode document)
    {
        if(filter == null)
            return true;

		boolean result = false;

		try
		{
			final Context cx = Context.enter();
			final Object doc = JSONUtilities.toJSONObject(cx, script, document.toString());
			result = Context.toBoolean(filter.call(cx, script, null, new Object[]{doc}));
		}
		finally
		{
			Context.exit();
		}
		
	    return result;
    }

	@Override
    public JsonNode transform(JsonNode document)
    {

	    if(transform == null)
        {
            log.trace("Passing document as is because no transformation function is configured.");
            return document;
        }

        try
        {
            final Context cx = Context.enter();
            final Object doc = JSONUtilities.toJSONObject(cx, script, document.toString());
            final Object result = transform.call(cx, script, null, new Object[]{doc});
            return JSONUtilities.fromJSONObject(new ObjectMapper(), cx, script, result);
        }
        catch (IOException e)
        {
            //TODO: and what about error handling???
            throw new RuntimeException("Could not transform the input document." , e);
        }
        finally
        {
            Context.exit();
        }
    }

	@Override
    public IndexConfig index()
    {
	    return indexConfig;
    }

    @Override
    public String targetType()
    {
        return name();
    }

    @Override
    public String sourceIndex()
    {
        return sourceIndex;
    }

    @Override
    public String sourceType()
    {
        return sourceType;
    }

    @Override
    public String targetIndex()
    {
        return index().name();
    }

    @Override
    public Map<String, WalkConfig> walks()
    {
	    return walks;
    }
}

class JavascriptWalkConfig implements WalkConfig
{
    final String walkName;
    final Direction direction;
    final TypeConfig typeCfg;

    // TODO use guava immutables
    final Map<String, JavascriptPropertyConfig> properties = new HashMap<String,JavascriptPropertyConfig>();


    public JavascriptWalkConfig(String walkName, Direction direction, TypeConfig typeCfg, Scriptable scope, Scriptable propertyScriptable)
    {
        this.walkName = walkName;
        this.direction = direction;
        this.typeCfg = typeCfg;

        try
        {
            Context.enter();

            // add all the properties
            for(Object id : ScriptableObject.getPropertyIds(propertyScriptable))
            {
                final String propertyName = id.toString();
                final Scriptable property = (Scriptable)ScriptableObject.getProperty(propertyScriptable, propertyName);

                final Function reduce = (Function)ScriptableObject.getProperty(property, "reduce");
                final boolean nested = ScriptableObject.getProperty(property, "nested").toString().equals("true");

                this.properties.put(propertyName, new JavascriptPropertyConfig(propertyName, nested, reduce, scope, this));
            }
        }
        finally
        {
            Context.exit();
        }
    }

    @Override
    public Direction direction()
    {
        return direction;
    }

    @Override
    public TypeConfig type()
    {
        return typeCfg;
    }

    @Override
    public Map<String,? extends PropertyConfig> properties()
    {
	    return properties;
    }

    @Override
    public String name()
    {
        return walkName;
    }
}


class JavascriptPropertyConfig implements PropertyConfig
{
	final String name;
    final boolean nested;
	final Function reduce;
	final Scriptable scope;
	final WalkConfig walkConfig;
	
	public JavascriptPropertyConfig(String name, boolean nested, Function reduce, Scriptable scope, WalkConfig walkConfig)
	{
        this.nested = nested;
		this.name = name;
		this.reduce = reduce;
		this.scope = scope;
		this.walkConfig = walkConfig;
	}
	
	@Override
	public String name()
	{
		return this.name;
	}

	@Override
	public JsonNode reduce(Tree<ResolvedPathElement> tree)
	{
		JsonNode result = null;

        final ObjectMapper om = new ObjectMapper();

        final com.google.common.base.Function<ResolvedPathElement, JsonNode> resultToString = new com.google.common.base.Function<ResolvedPathElement, JsonNode>()
        {
            @Override
            public JsonNode apply(ResolvedPathElement input)
            {
                try
                {
                    final Optional<GetResponse> getResponse = input.getResponse();
                    if(getResponse.isPresent())
                    {
                        final String getResponseString = getResponse.get().getSourceAsString();
                        final Edge edge = input.edge();
                        final Vertex vertex = input.vertex();

                        final ObjectNode n = om.createObjectNode();
                        n.put("exists", true);
                        n.put("value", om.readTree(getResponseString));
                        if (edge != null)
                            n.put("edge", JSONUtilities.toJSON(om, edge));
                        n.put("vertex", JSONUtilities.toJSON(om, vertex));
                        return n;
                    }
                    else
                    {
                        final ObjectNode n = om.createObjectNode();
                        n.put("exists", false);
                        return n;
                    }
                }
                catch (IOException e)
                {
                    final ObjectNode n = om.createObjectNode();
                    n.put("exception", e.getClass().getCanonicalName());
                    n.put("message", e.getMessage());
                    return n;
                }
            }
        };

        final Tree<JsonNode> jsonTree = Trees.map(resultToString, tree);
		
		try
		{
			final Context cx = Context.enter();

            final JsonNode jtree = Trees.toJsonTree(om, jsonTree);

            final Object jsobject = JSONUtilities.toJSONObject(cx, scope, jtree.toString());

            // call our "reduction" function
			final Object obj = reduce.call(cx, scope, null, new Object[] { jsobject });
			
			// TODO this is inefficient and silly...
			
			// convert javascript object to JSON string
			final String objectJson = (String) NativeJSON.stringify(cx, scope, obj, null, null);
			
			// convert JSON string to JsonNode
			result = om.readTree(objectJson);
		}
		catch (JsonProcessingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			Context.exit();
		}

		return result;
	}

	@Override
    public WalkConfig walk()
    {
	    return walkConfig;
    }
}
