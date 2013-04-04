/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package dgm.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * User: rico
 * Date: 04/04/2013
 */
public class ConfigurationChange
{
    private List<String> indexesAdded = new ArrayList<String>();
    private List<String> indexesRemoved = new ArrayList<String>();
    private List<String> indexesChanged = new ArrayList<String>();

    public ConfigurationChange()
    {}

    public void indexAdded(String index)
    {
        indexesAdded.add(index);
    }

    public void indexRemoved(String index)
    {
        indexesRemoved.add(index);
    }

    public void indexChanged(String index)
    {
        indexesChanged.add(index);
    }

    public Iterable<String> indexesAdded()
    {
        return indexesAdded;
    }

    public Iterable<String> indexesRemoved()
    {
        return indexesRemoved;
    }

    public Iterable<String> indexesChanged()
    {
        return indexesChanged;
    }

    public boolean hasChanges()
    {
        return 0 != (indexesAdded.size() + indexesChanged.size() + indexesRemoved.size());
    }
}
