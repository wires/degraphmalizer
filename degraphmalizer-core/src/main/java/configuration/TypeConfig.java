package configuration;

import com.fasterxml.jackson.databind.JsonNode;
import graphs.ops.Subgraph;

import java.util.Map;

public interface TypeConfig
{
	String name();

	/**
	 * Function that given the document computes it's generated subgraph
	 * 
	 * @param document
	 */
    Subgraph extract(JsonNode document);

	/**
	 * Process the document before it is inserted into ES
	 *
	 * @param document
	 * @return the process document as you want it inserted into ES
	 */
	JsonNode transform(JsonNode document);


    /**
     * Return false if the document shouldn't be processed by the degraphmalizer
     *
     * @param document
     * @return
     */
	boolean filter(JsonNode document);
	
	/**
	 * A type is always part of a {@link IndexConfig}
	 */
	IndexConfig index();

    /**
     * The index to which the denormalized documents should be written.
     */
    String targetIndex();

    /**
     * The type which denormalized documents should be assigned.
     */
    String targetType();

    // match on this index
    String sourceIndex();

    // match this type
    String sourceType();

    /**
	 * Walks performed for this type
	 */
	Map<String, WalkConfig> walks();
}
