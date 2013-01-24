package jmx;

public interface GraphBuilderMBean
{	
	/**
	 * Query elasticsearch for a document
	 */
    String esGet(String index, String type, String id);
	
    /**
     * Open a groovy console, with objects <code>es</code> and <code>graph</code> representing 
     * the elasticsearch client and blueprints graph respectively.
     */
    void openConsole();

    void dumpGraph();
}
