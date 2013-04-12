package dgm.trees;

/**
 * Created with IntelliJ IDEA.
 * User: wires
 * Date: 3/20/13
 * Time: 12:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class CycleKiller<A> implements TreeVisitor<A>
{
    final TreeVisitor<A> adaptee;
    final OccurrenceTracker<A> occurrenceTracker;

    public CycleKiller(TreeVisitor<A> adaptee, OccurrenceTracker<A> occurrenceTracker)
    {
        this.adaptee = adaptee;
        this.occurrenceTracker = occurrenceTracker;
    }

    @Override
    public boolean visitNode(A node, TreeViewer<A> viewer)
    {
        // stop the recursion when maximum level is reached
        final boolean shouldStop = occurrenceTracker.hasOccurred(node);
        return shouldStop || adaptee.visitNode(node, viewer);
    }

    @Override
    public void beginChildren(A node, TreeViewer<A> viewer)
    {
        adaptee.beginChildren(node, viewer);
    }

    @Override
    public void endChildren(A node, TreeViewer<A> viewer)
    {
        adaptee.endChildren(node, viewer);
    }
}
