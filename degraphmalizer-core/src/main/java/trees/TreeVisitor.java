package trees;

public interface TreeVisitor<T>
{
	void visitNode(T node, TreeViewer<T> viewer);
	
	void beginChildren(T node, TreeViewer<T> viewer);
	
	void endChildren(T node, TreeViewer<T> viewer);
}
