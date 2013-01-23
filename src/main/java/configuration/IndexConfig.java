package configuration;

import java.util.Map;

public interface IndexConfig {
    /**
     * Work on some index
     *
     * @return name of the index (as used by ES)
     */
    public String name();

    /**
     * Get all registered types
     *
     * @return Mapping of type name (as used by ES) to type config
     */
    public Map<String, ? extends TypeConfig> types();
}