package dgm.graphs.ops;

import dgm.degraphmalizr.ID;
import dgm.exceptions.DegraphmalizerException;

public interface SubgraphManager
{
    void commitSubgraph(ID id, Subgraph subgraph) throws DegraphmalizerException;

    void deleteSubgraph(ID id) throws DegraphmalizerException;
}