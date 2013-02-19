package degraphmalizr;

public interface Dgm<G, Vx, ID>
{
    ID getID(Vx v);
    Iterable<Vx> findByID(G graph, ID id);
    Iterable<Vx> vertices(G graph);
}

// forall w in vertices(G), forall v in vertices(neighbourhood(G,w)),
//                                 w in vertices(coneighbourhood(G,v))
interface Traversal<G, Vx>
{
    G neighbourhood(G graph, Vx root);
    G coneighbourhood(G graph, Vx root);
}
