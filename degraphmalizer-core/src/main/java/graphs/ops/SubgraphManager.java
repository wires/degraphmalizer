package graphs.ops;

import degraphmalizr.ID;
import exceptions.DegraphmalizerException;

public interface SubgraphManager
{
    void commitSubgraph(ID id, Subgraph subgraph) throws DegraphmalizerException;

    void deleteSubgraph(ID id) throws DegraphmalizerException;
}