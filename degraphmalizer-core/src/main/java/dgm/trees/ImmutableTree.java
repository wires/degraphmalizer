package dgm.trees;

import java.util.*;

public class ImmutableTree<A> implements Tree<A>
{
    protected final A value;
    protected final Iterable<Tree<A>> children;

    @Override
    public A value()
    {
        return value;
    }

    @Override
    public Iterable<Tree<A>> children()
    {
        return children;
    }

    public ImmutableTree(A value, Tree<A>... children)
    {
        this(value, Arrays.asList(children));
    }

    public ImmutableTree(final A value, final Iterable<Tree<A>> children)
    {
        this.value = value;
        this.children = children;
    }
}
