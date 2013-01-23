package trees;

public interface TreeVisitor<T>
{
	public void visitNode(T node, TreeViewer<T> viewer);
	
	public void beginChildren(T node, TreeViewer<T> viewer);
	
	public void endChildren(T node, TreeViewer<T> viewer);
}
