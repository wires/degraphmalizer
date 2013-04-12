package dgm.trees;

import java.util.Arrays;

/**
 * Combine multiple tree visitors.
 *
 * Visitors are combined such that if (at least) <emph>one</emph> visitor decides to abort further visit to a node,
 * this composite will too.
 *
 */
public class CompositeTreeVisitor<T> implements TreeVisitor<T>
{
    final Iterable<TreeVisitor<T>> visitors;

    public CompositeTreeVisitor(TreeVisitor<T>... visitors)
    {
        this(Arrays.asList(visitors));
    }

    public CompositeTreeVisitor(Iterable<TreeVisitor<T>> visitors)
    {
        this.visitors = visitors;
    }

    @Override
    public boolean visitNode(T node, TreeViewer<T> viewer)
    {
        boolean state = false;

        // combine using "OR" operator
        for(TreeVisitor<T> v : visitors)
            state |= v.visitNode(node, viewer);

        return state;
    }

    @Override
    public void beginChildren(T node, TreeViewer <T> viewer)
    {
        for(TreeVisitor<T> v : visitors)
            v.beginChildren(node, viewer);
    }

    @Override
    public void endChildren(T node, TreeViewer<T> viewer)
    {
        for(TreeVisitor<T> v : visitors)
            v.endChildren(node, viewer);
    }
}
