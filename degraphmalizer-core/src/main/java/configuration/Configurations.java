package configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Configurations
{
    final static private Logger log = LoggerFactory.getLogger(Configurations.class);

    /**
     * Find all TypeConfigs with specified source index and source type
     */
    public static Iterable<TypeConfig> configsFor(Configuration cfg, String srcIndex, String srcType)
    {
        StringBuilder logMessage = null;

        if(log.isDebugEnabled())
        {
            logMessage = new StringBuilder("Matching request for /");
            logMessage.append(srcIndex).append("/").append(srcType);
            logMessage.append(" to [");
        }

        final ArrayList<TypeConfig> configs = new ArrayList<TypeConfig>();

        // find all matching configs
        for(IndexConfig i : cfg.indices().values())
            for(TypeConfig t : i.types().values())
                if(srcIndex.equals(t.sourceIndex()) && srcType.equals(t.sourceType()))
                {
                    if((logMessage != null))
                    {
                        logMessage.append(" /").append(t.targetIndex());
                        logMessage.append("/").append(t.targetType());
                        logMessage.append(", ");
                    }

                    configs.add(t);
                }

        if(logMessage != null)
            log.debug(logMessage.append("]").toString());

        return configs;
    }
}