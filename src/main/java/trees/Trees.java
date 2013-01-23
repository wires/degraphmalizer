package trees;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.elasticsearch.common.Nullable;
import scala.collection.TraversableOnce;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;

/**
 * Helper functions for {@link Tree}s
 * 
 * @author wires
 *
 */
public class Trees
{
	public static <A> void printTree(Tree<A> tree, PrintStream ps)
	{
		ps.printf("(%s (", tree.value());
		for (Tree<A> t : tree.children())
		{
			printTree(t, ps);
			ps.print(",");
		}
		ps.print("))");
	}

    /**
     * Turn a {@code Tree} of {@code Optional}s into a {@code Optional}al {@code Tree}, meaning that if there is one
     * absent value in the tree, the whole tree is absent.
     */
    public static <A> Optional<Tree<A>> optional(Tree<Optional<A>> treeOfOptionals)
    {
        class ValueIsAbsent extends RuntimeException {};

        final Function<Optional<A>, A> nonAbsent = new Function<Optional<A>, A>()
        {
            @Override
            public A apply(@Nullable Optional<A> input)
            {
                if(!input.isPresent())
                    throw new ValueIsAbsent();

                return input.get();
            }
        };

        try
        {
            return Optional.of(map(nonAbsent, treeOfOptionals));
        }
        catch (ValueIsAbsent e)
        {
            return Optional.absent();
        }
    }

    /**
	 * Map a function over a tree
	 * 
	 * @param fn
	 * @param tree Tree of {@code A}'s
	 * @return Tree of {@code B}'s
	 */
	public static <A,B> Tree<B> map(final Function<A,B> fn, Tree<A> tree)
	{
		final B value = fn.apply(tree.value);
		
		if (tree.isLeaf())
			return new Tree<B>(value);
		else
		{
			final Function<Tree<A>,Tree<B>> tmap = new Function<Tree<A>,Tree<B>>()
				{
					public Tree<B> apply(Tree<A> tree)
					{
						return map(fn, tree);
					}
				};
			
			final Collection<Tree<B>> tb = Collections2.transform((Collection<Tree<A>>) tree.children, tmap);
			return new Tree<B>(value, tb);
		}
	}
	
	/**
	 * Turn a {@link Function} into an {@link Callable}
	 * 
	 * @param fn
	 * @return
	 */
	public static <A,B> Function<A,Callable<B>> mkAsync(final Function<A,B> fn)
	{
		return new Function<A,Callable<B>>()
			{
				public Callable<B> apply(final A a)
				{
					final Callable<B> g = new Callable<B>()
						{
							@Override
							public B call() throws Exception
							{
								return fn.apply(a);
							}
						};
					
					return g;
				}
			};
	}
	
	/**
	 * In parallel, map a function over a tree
	 * 
	 * @param executor Where to submit the jobs to
	 * @param fn The function applied to every node in the tree
	 * @param tree The tree to map
	 * @return
	 * @throws InterruptedException
	 */
	public static <A,B> Tree<B> pmap(final ExecutorService executor, final Function<A,B> fn, Tree<A> tree)
	        throws InterruptedException
	{
		// convert the tree into jobs, and submit them to the executor
		final Function<A,Future<B>> toJob = new Function<A,Future<B>>()
			{
				public Future<B> apply(final A a)
				{
					return executor.submit(new Callable<B>()
						{
							@Override
                            public B call() throws Exception
                            {
	                            return fn.apply(a);
                            }
						});
				}
			};
		
		// get automatically waits for jobs to finish
		final Function<Future<B>, B> waitDone = new Function<Future<B>, B>()
			{
				public B apply(final Future<B> b)
				{
					try
                    {
	                    return b.get();
                    }
                    catch (InterruptedException e)
                    {
	                   throw new RuntimeException(e);
                    }
                    catch (ExecutionException e)
                    {
                    	throw new RuntimeException(e);
                    }
				}
			};

		return map(waitDone, map(toJob, tree));
	}

	
	/**
	 * Convert a tree of JsonNode's into a Json tree like so:
	 * 
	 * <pre>
	 * { "value": NODE_VALUE,
	 *   "children: [
	 *   	{
	 *      	"value": CHILD1_VALUE,
	 *          "children": [ ... ]
	 *      },
	 *      {
	 *           "value": CHILD2_VALUE,
	 *           "children": [....]
	 *      }
	 *   ]
	 * }
	 * </pre>
	 */
	// TODO all this is very memory inefficient and will lead to stack overflows for very deep trees
	public static ObjectNode toJsonTree(ObjectMapper objectMapper, Tree<? extends JsonNode> tree)
	{
		final ObjectNode node = objectMapper.createObjectNode();
		node.put("_value", tree.value());
		
		final ArrayNode children = objectMapper.createArrayNode();
		for (Tree<? extends JsonNode> child : tree.children())
			children.add(toJsonTree(objectMapper, child));
		
		node.put("_children", children);
		
		return node;
	}
	
	/** Helper method to directly walk a TreeNode instance */
	public static <T> Iterable<T> bfsWalk(Tree<T> root)
	{
		// view TreeNode as tree
		final TreeViewer<Tree<T>> viewer = new TreeViewer<Tree<T>>()
			{
				@Override
				public Iterable<Tree<T>> children(Tree<T> node)
				{
					return node.children();
				}
			};
		
		// get value from a tree
		final Function<Tree<T>,T> getValue = new Function<Tree<T>,T>()
			{
				public T apply(Tree<T> node)
				{
					return node.value();
				}
			};
		
		final Iterable<Tree<T>> ti = bfsWalk(root, viewer);
		
		return Iterables.transform(ti, getValue);
	}
	
	public static <A> Iterable<A> bfsWalk(final A root, final TreeViewer<A> viewer)
	{
		return new Iterable<A>()
			{
				@Override
				public Iterator<A> iterator()
				{
					return new Iterator<A>()
						{
							final Queue<A> q = new LinkedList<A>(Collections.singleton(root));
							
							@Override
							public boolean hasNext()
							{
								return !q.isEmpty();
							}
							
							@Override
							public A next()
							{
								final A a = q.poll();
								
								// add children to end of the queue
								for (A c : viewer.children(a))
									q.offer(c);
								
								return a;
							}
							
							@Override
							public void remove()
							{
								// TODO use some sort of standard
								// "NotImplemented" exception (how does guava do
								// this?)
								throw new RuntimeException("Not Implemented");
							}
						};
				}
			};
	}
	
	enum EventType
	{
		VISIT_NODE, BEGIN_CHILDREN, END_CHILDREN
	};
	
	/**
	 * BFS visit tree
	 * 
	 * @param root
	 * @param viewer
	 * @param visitor
	 */
	public static <A> void bfsVisit(final A root, final TreeViewer<A> viewer, final TreeVisitor<A> visitor)
	{
		class VisitEvent
		{
			public final A node;
			public final EventType eventType;
			public final VisitEvent otherSide;
			
			public VisitEvent(EventType eventType, A node, VisitEvent other)
			{
				this.node = node;
				this.eventType = eventType;
				this.otherSide = other;
			}
			
			// return true if event type == BEGIN_NODE/END_NODE
			public boolean callVisitor(TreeVisitor<A> visitor)
			{
				switch (eventType)
				{
					case BEGIN_CHILDREN:
						visitor.beginChildren(node, viewer);
						return true;
					case END_CHILDREN:
						visitor.endChildren(node, viewer);
						return true;
					case VISIT_NODE:
						visitor.visitNode(node, viewer);
						return false;
					default:
						throw new RuntimeException("OMGWTF Unreachable code reached...");
				}
			}
		}
		
		final LinkedList<VisitEvent> q = new LinkedList<VisitEvent>();
		q.offer(new VisitEvent(EventType.VISIT_NODE, root, null));
		
		VisitEvent insertionPt = null;
		
		while (!q.isEmpty())
		{
			final VisitEvent a = q.poll();
			
			// update the insertion point to the END_CHILDREN node
			if (a.eventType == EventType.BEGIN_CHILDREN)
				insertionPt = a.otherSide;
			
			// call visitor interface
			if (a.callVisitor(visitor))
				// nothing further to be done for marker event (BEGIN/END)
				continue;
			
			
			final VisitEvent end = new VisitEvent(EventType.END_CHILDREN, a.node, null);
			final VisitEvent start = new VisitEvent(EventType.BEGIN_CHILDREN, a.node, end);
			
			insertBefore(q, start, insertionPt);
			
			// add children to end of the queue
			for (A c : viewer.children(a.node))
				insertBefore(q, new VisitEvent(EventType.VISIT_NODE, c, null), insertionPt);
			
			insertBefore(q, end, insertionPt);
		}
	}
	
	private final static <A> void insertBefore(LinkedList<A> ll, A value, A before)
	{
		if (before == null)
			ll.offer(value);
		else
		{
			final int i = ll.indexOf(before);
			ll.add(i, value);
		}
	}
	
}