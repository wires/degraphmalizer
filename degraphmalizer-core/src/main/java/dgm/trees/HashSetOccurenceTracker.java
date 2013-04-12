package dgm.trees;

import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: wires
 * Date: 3/20/13
 * Time: 12:50 AM
 * To change this template use File | Settings | File Templates.
 */
class HashSetOccurenceTracker<A> implements OccurrenceTracker<A>
{
    final HashSet<A> contents = new HashSet<A>();

    @Override
    public boolean hasOccurred(A element)
    {
        final boolean in = contents.contains(element);

        // keep track of every element we see
        if(!in)
            contents.add(element);

        return in;
    }
}
