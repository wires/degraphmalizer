package trees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
public class Tree<A>
{
	final A value;
	final List<Tree<A>> children;
	
	public Tree(A value, Collection<Tree<A>> children)
	{
		this.value = value;
		this.children = new ArrayList<Tree<A>>(children);
	}
	
    public Tree(A value, Tree<A>... children)
	{
		this.value = value;
		this.children = Arrays.asList(children);
	}
	
	public Tree(A leaf)
	{
		this.value = leaf;
		this.children = Collections.emptyList();
	}

	public final Iterable<Tree<A>> children()
	{
		return this.children;
	}
	
	public final A value()
	{
		return this.value;
	}
	
	public final boolean isLeaf()
	{
		return this.children.size() == 0;
	}
}
