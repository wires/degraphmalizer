package graphs.ops;

import degraphmalizr.ID;
import exceptions.DegraphmalizerException;

public interface SubgraphManager
{
    Subgraph createSubgraph(ID id) throws DegraphmalizerException;

    /**
     * @throws DegraphmalizerException when you try to merge a vertex into the graph that has an older version,
     * or when is is not possible to change the owner of a vertex
     * TODO: perhaps this should raise different exceptions
     */
    void commitSubgraph(Subgraph subgraph) throws DegraphmalizerException;

    void deleteSubgraph(Subgraph subgraph) throws DegraphmalizerException;
}