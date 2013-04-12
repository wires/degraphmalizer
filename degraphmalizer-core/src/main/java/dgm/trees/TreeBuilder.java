package dgm.trees;

import java.util.*;

/** Build a tree by visiting it
 */
public class TreeBuilder<A> implements TreeVisitor<A>
{
    final Deque<List<Tree<A>>> trees = new LinkedList<List<Tree<A>>>();
    Tree<A> root = null;

    @Override
    public boolean visitNode(A node, TreeViewer<A> viewer)
    {
        return false;
    }

    @Override
    public void beginChildren(A node, TreeViewer<A> viewer)
    {
        trees.addFirst(new ArrayList<Tree<A>>());
    }

    @Override
    public void endChildren(A node, TreeViewer<A> viewer)
    {
        final List<Tree<A>> children = trees.removeFirst();
        final Tree<A> tree = new ImmutableTree<A>(node, children);

        // we have not reached the top of the queue
        final List<Tree<A>> parent = trees.peekFirst();
        if(parent != null)
        {
            parent.add(tree);
            return;
        }

        // we finished visiting, store the root
        root = tree;
    }

    public Tree<A> tree()
    {
        return root;
    }
}
