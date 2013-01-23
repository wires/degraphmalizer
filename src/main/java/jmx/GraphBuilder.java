package jmx;

import javax.inject.Inject;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;

import com.tinkerpop.blueprints.Graph;

public class GraphBuilder implements GraphBuilderMBean
{
	@Inject
	Logger log;
	
	protected final Client client;
	protected final Graph graph;
	
	@Inject
	public GraphBuilder(Client client, Graph graph)
	{
		this.client = client;
		this.graph = graph;
	}
	
	@Override
	public String esGet(String index, String type, String id)
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
	
}
