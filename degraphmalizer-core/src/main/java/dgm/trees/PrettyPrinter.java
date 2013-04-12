package dgm.trees;


import com.google.common.base.Function;
import dgm.trees.TreeViewer;
import dgm.trees.TreeVisitor;

public class PrettyPrinter<T> implements TreeVisitor<T>
{
    protected final Function<T,String> show;
    protected int currentLevel = 0;
    protected StringBuilder currentOutput = new StringBuilder();

    public PrettyPrinter(Function<T,String> show)
    {
        this.show = show;
    }

    protected void doIndent()
    {
        for(int i = 0; i < currentLevel; i++)
            currentOutput.append("  ");
    }


    @Override
    public boolean visitNode(T node, TreeViewer<T> viewer)
    {
        doIndent();
        currentOutput.append(show.apply(node));
        currentOutput.append('\n');

        // keep recursions going
        return false;
    }

    @Override
    public void beginChildren(T node, TreeViewer<T> viewer)
    {
        currentLevel++;
    }

    @Override
    public void endChildren(T node, TreeViewer<T> viewer)
    {
        currentLevel--;
    }

    public String toString()
    {
        return currentOutput.toString();
    }
}
