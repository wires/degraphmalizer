package dgm.api;

import com.google.common.collect.Sets;

import java.util.Set;

interface User<J,S,U>
{
    S extract(J document);
    J reduce(J document, U context);
}

interface Codep<D,G,U>
{
    U traverse(D id, G graph);
    U cotraverse(D id, G graph);

    Set<D> contents(U context);
}

interface Indexer<D,J>
{
    J get(D id);
    void put(D id, J document);
}

interface SubgraphManager<D,S,G>
{
    G current();
    G delete(D id);
    G update(D id, S subgraph);
}


interface Degraphmalizer<D,J>
{
    Set<D> degraphmalize(D id);
    J recompute(D id);
}

interface Backend<D,J,S,G,U>
{
    Codep<D,G,U> codep();
    Indexer<D,J> im();
    SubgraphManager<D,S,G> sgm();
    User<J,S,U> user(D id);
}

class DgmCore<D,J,S,G,U> implements Degraphmalizer<D,J>
{
    final Backend<D,J,S,G,U> backend;

    public DgmCore(Backend<D, J, S, G, U> backend)
    {
        this.backend = backend;
    }

    public Set<D> degraphmalize(D id)
    {
        // extract subgraph
        final J source = backend.im().get(id);
        final S subgraph = backend.user(id).extract(source);

        // compute context before graph change
        final G g1 = backend.sgm().current();
        final U ctxPre = backend.codep().cotraverse(id, g1);
        final Set<D> pre = backend.codep().contents(ctxPre);

        // commit subgraph
        final G g2 = backend.sgm().update(id, subgraph);

        // compute context after graph change
        final U ctxPost = backend.codep().cotraverse(id, g2);
        final Set<D> post = backend.codep().contents(ctxPost);

        return Sets.union(pre, post);
    }

    public J recompute(D id)
    {
        // extract subgraph
        final J source = backend.im().get(id);

        // compute context
        final G g = backend.sgm().current();
        final U ctx = backend.codep().cotraverse(id, g);

        final J target = backend.user(id).reduce(source, ctx);

        backend.im().put(id, target);

        return target;
    }
}