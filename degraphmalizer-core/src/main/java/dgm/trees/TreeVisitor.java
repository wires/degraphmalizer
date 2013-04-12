package dgm.trees;

public interface TreeVisitor<T>
{
    // return true if you don't want to continue visiting this node
	boolean visitNode(T node, TreeViewer<T> viewer);
	
	void beginChildren(T node, TreeViewer<T> viewer);
	
	void endChildren(T node, TreeViewer<T> viewer);
}
