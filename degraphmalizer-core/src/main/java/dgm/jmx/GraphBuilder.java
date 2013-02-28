package dgm.jmx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.Graph;
import dgm.graphs.GraphQueries;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;

import javax.inject.Inject;

public class GraphBuilder implements GraphBuilderMBean
{
	@Inject
	Logger log;
	
	protected final Client client;
	protected final Graph graph;
    protected final ObjectMapper om;
	
	@Inject
	public GraphBuilder(ObjectMapper om, Client client, Graph graph)
	{
		this.client = client;
		this.graph = graph;
        this.om = om;
	}
	
	@Override
	public final String esGet(String index, String type, String id)
	{
		try
        {
	        final GetResponse gr = client.prepareGet(index, type, id).execute().actionGet();
	        
	        if(!gr.exists())
	        	return "Doesn't exist";
	        
	        return gr.getSourceAsString();
        }
        catch (ElasticSearchException e)
        {
        	log.debug("ElasticSearchException {}", e);
	
        	return e.getMessage();
        }
	}
	
	@Override
	public void openConsole()
	{
        // TODO move groovy console into groovy package

        /*
        import groovy.lang.Binding;
        import groovy.ui.Console;

		final Binding binding = new Binding();
		binding.setVariable("es", this.client);
		binding.setVariable("graph", this.graph);
		
		final Console console = new Console(ClassLoader.getSystemClassLoader(), binding);
		console.run();
	    */
	}

    @Override
    public final void dumpGraph() {
        GraphQueries.dumpGraph(om, graph);
    }
}
