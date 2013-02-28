package dgm.streaming;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

class RandomGraph extends TinkerGraph
{
    static final long serialVersionUID = 1;

    /**
     *
     * @param N hoeveel nodes
     * @param edgeProbability
     */
    public RandomGraph(int N, float edgeProbability)
    {
        for(int i = 0; i < N; i++)
        {
            final Vertex v = addVertex(Integer.toString(i));
            v.setProperty("label", Integer.toHexString(i));

            if (Math.random() > edgeProbability)
            {
                final int a = (int) Math.floor(Math.random() * i);
                final int b = (int) Math.floor(Math.random() * i);

                if(a != b)
                {
                    final Vertex va = getVertex(Integer.toString(a));
                    final Vertex vb = getVertex(Integer.toString(b));

                    if((va == null) || (vb == null))
                        continue;

                    addEdge("e" + Integer.toOctalString(i), va, vb, "edge");
                }
            }
        }
    }
}
