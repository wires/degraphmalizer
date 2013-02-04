package configuration;

import exceptions.DegraphmalizerException;

import java.util.ArrayList;

public class Configurations
{
    /**
     * Find all TypeConfigs with specified source index and source type
     */
    public static Iterable<TypeConfig> configsFor(Configuration cfg, String srcIndex, String srcType) throws DegraphmalizerException
    {
        final ArrayList<TypeConfig> configs = new ArrayList<TypeConfig>();

        for(IndexConfig i : cfg.indices().values())
            for(TypeConfig t : i.types().values())
                if(srcIndex.equals(t.sourceIndex()) && srcType.equals(t.sourceType()))
                    configs.add(t);

        return configs;
    }
}