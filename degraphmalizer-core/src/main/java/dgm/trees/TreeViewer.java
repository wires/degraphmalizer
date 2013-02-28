package dgm.trees;

/**
 * View objects of type <code>A</code> as a tree
 * 
 * @author wires
 *
 * @param <A>
 */
public interface TreeViewer<A>
{
	/**
	 * Get all children of <code>node</code>
	 * 
	 * @param node
	 * @return
	 */
	Iterable<A> children(A node);
}
