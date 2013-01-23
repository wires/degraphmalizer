package configuration;

import com.fasterxml.jackson.databind.JsonNode;
import graphs.ops.Subgraph;
import degraphmalizr.jobs.DegraphmalizeAction;

import java.util.Map;

public interface TypeConfig
{
	public String name();
	
	/**
	 * Function that given the document computes it's generated subgraph
	 * 
	 * @param job
	 * @param subgraph
	 */
	public void extract(DegraphmalizeAction job, Subgraph subgraph);

	/**
	 * Process the document before it is inserted into ES
	 *
	 * @param document
	 * @return the process document as you want it inserted into ES
	 */
	public JsonNode transform(JsonNode document);


    /**
     * Return false if the document shouldn't be processed by the degraphmalizer
     *
     * @param document
     * @return
     */
	public boolean filter(JsonNode document);
	
	/**
	 * A type is always part of a {@link IndexConfig}
	 */
	public IndexConfig index();

    /**
     * The index to which the denormalized documents should be written.
     */
    public String targetIndex();

    /**
     * The type which denormalized documents should be assigned.
     */
    public String targetType();

    // match on this index
    public String sourceIndex();

    // match this type
    public String sourceType();

    /**
	 * Walks performed for this type
	 */
	public Map<String, WalkConfig> walks();

}
