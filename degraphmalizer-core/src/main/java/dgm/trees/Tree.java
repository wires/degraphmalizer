package dgm.trees;

/** A tree
 * 
 * Each node stores a value and a list of child trees.
 * 
 * <pre>
 *           .--(value)-->[]
 *          /
 * (value)-+----(value)-->[]   .--(value)-->[]
 *          \                 /
 *           `--(value)------+----(value)-->[]
 * 
 * </pre>
 * 
 * @author wires
 *
 * @param <A>
 */
public interface Tree<A>
{
	A value();
	Iterable<Tree<A>> children();
}
