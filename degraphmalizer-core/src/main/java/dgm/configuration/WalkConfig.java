package dgm.configuration;

import com.tinkerpop.blueprints.Direction;

import java.util.Map;

/**
 * Any walk that can be reversed could function as an input to the parameter
 * reducer.
 * <p/>
 * However, not every language for defining graph walks is invertible. So to not
 * complicate things for the moment we restrict to forward and backward walks
 * which are each other's reverse.
 *
 * @author wires
 */
public interface WalkConfig {
    /**
     * The direction of the graph walk.
     * <p/>
     * At the moment Graph walks are restricted to forward or backward walks.
     * <p/>
     * <ul>
     * <li>Direction.IN means we follow edges from head to tail</li>
     * <li>Direction.OUT means we follow edges from tail to head</li>
     * </ul>
     *
     * @return
     */
    Direction direction();


    /**
     * A walk is always configured against a {@link TypeConfig}
     *
     * @return
     */
    TypeConfig type();


    /**
     * List of named {@link PropertyConfig}s
     *
     * @return
     */
    Map<String, ? extends PropertyConfig> properties();

    /**
     * Name of the walk
     */
    String name();
}