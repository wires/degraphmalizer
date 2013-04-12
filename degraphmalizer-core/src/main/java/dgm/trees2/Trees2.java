package dgm.trees2;

import dgm.trees.TreeViewer;
import dgm.trees.TreeVisitor;

import java.util.LinkedList;


public class Trees2
{
   private enum Token
   {
       NODE {
           public <A> boolean visit(A node, TreeViewer<A> viewer, TreeVisitor<A> visitor) {
               return visitor.visitNode(node, viewer);
          }
       },

       BEGIN {
           public <A> boolean visit(A node, TreeViewer<A> viewer, TreeVisitor<A> visitor) {
               visitor.beginChildren(node, viewer);
               return true;
           }
       },

       END {
           public <A> boolean visit(A node, TreeViewer<A> viewer, TreeVisitor<A> visitor) {
               visitor.endChildren(node, viewer);
               return true;
           }
       };

       // return false if the Token is a "marker", ie. BEGIN or END
       abstract <A> boolean visit(A node, TreeViewer<A> viewer, TreeVisitor<A> visitor);
   }

    static class Event<A>
    {
        public final A node;
        public final Token token;
        public final Event<A> otherSide;

        private Event(Token token, A node, Event<A> other)
        {
            this.node = node;
            this.token = token;
            this.otherSide = other;
        }

        public boolean callVisitor(TreeViewer<A> viewer, TreeVisitor<A> visitor) {
            return token.visit(node, viewer, visitor);
        }

        static <A> Event<A> begin(A node, Event<A> other) {
            return new Event<A>(Token.BEGIN, node, other);
        }

        static <A> Event<A> end(A node) {
            return new Event<A>(Token.END, node, null);
        }

        static <A> Event<A> node(A node) {
            return new Event<A>(Token.NODE, node, null);
        }

    }

    /**
     * BFS visit tree.
     *
     * Suppose we have this tree.
     *
     * <pre>
     *
     *      ,--(1)
     *    /
     * -(0)----(2)
     *    \
     *     `---(3)
     *
     * </pre>
     *
     * bfsVisit will call on {@link TreeVisitor} the following methods, in roughly this order:
     *
     * <pre>
     *  - visit v[0]
     *  - beginChildren v[0]
     *  - visit v[2]
     *  - visit v[3]
     *  - visit v[1]
     *  - beginChildren v[2]
     *  - endChildren v[2]
     *  - beginChildren v[3]
     *  - endChildren v[3]
     *  - beginChildren v[1]
     *  - endChildren v[1]
     *  - endChildren v[0]
     * </pre>
     */
    public static <A> void bfsVisit(final A root, final TreeViewer<A> viewer, final TreeVisitor<A> visitor)
    {
        // algorithm as follows:
        // - put first node on the tree
        // - visit that node
        // - put begin on queue
        //     visit all nodes
        // - put end on queue

        final LinkedList<Event<A>> q = new LinkedList<Event<A>>();
        q.offer(Event.node(root));

        Event<A> insertionPt = null;

        while (!q.isEmpty())
        {
            final Event<A> a = q.poll();

            // update the insertion point to the END node
            if (a.token == Token.BEGIN)
                insertionPt = a.otherSide;

            // call visitor interface
            if (a.callVisitor(viewer, visitor))
                // nothing further to be done for marker event (BEGIN/END)
                continue;

            final Event<A> end = Event.end(a.node);
            final Event<A> start = Event.begin(a.node, end);

            insertBefore(q, start, insertionPt);

            // add children to end of the queue
            for (A c : viewer.children(a.node))
                insertBefore(q, Event.node(c), insertionPt);

            insertBefore(q, end, insertionPt);
        }
    }

    private static <A> void insertBefore(LinkedList<A> ll, A value, A before)
    {
        if (before == null)
        {
            ll.addLast(value);
            return;
        }

        final int i = ll.indexOf(before);
        ll.add(i, value);
    }
}