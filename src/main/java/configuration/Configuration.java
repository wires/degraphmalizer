package configuration;

import java.util.Map;

public interface Configuration
{
    /**
     * Get all registered indices
     *
     * @return Mapping of index name (as used by ES) to index config
     */
    public Map<String,? extends IndexConfig> indices();
}
