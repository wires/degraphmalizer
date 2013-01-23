package configuration;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.action.get.GetResponse;
import trees.Tree;

/**
 * We start with a raw document and then add "derived" or "denormalized" values.
 * <p/>
 * A Property represents such a denormalized value. The value is computed by a
 * reduction on a graph walk. The result will typically be a list of documents.
 *
 * @author wires
 */
public interface PropertyConfig {
    /**
     * The name of the property. The denormalized value is added to the
     * <i>raw</i> document under this name.
     * <p/>
     * This must be unique per document type.
     *
     * @return
     */
    public String name();

    /**
     * The reduction function.
     * <p/>
     * Given a tree of documents (determined by the graph walk), we produce the
     * denormalized JSON value.
     *
     * @param tree The root of the tree is the <i>raw</i> document itself.
     */
    public JsonNode reduce(Tree<GetResponse> tree);

    /**
     * A property is always part of a {@link WalkConfig}
     */
    public WalkConfig walk();
}
