package dgm.streaming.command;

import java.util.Collections;
import java.util.Map;

public class GraphNode {

    private final String name;
    private final Map<String, Object> properties;

    GraphNode(String name, Map<String, Object> properties) {
        this.name = name;
        this.properties = properties;
    }

    public final String getName() {
        return name;
    }

    public final Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
}
