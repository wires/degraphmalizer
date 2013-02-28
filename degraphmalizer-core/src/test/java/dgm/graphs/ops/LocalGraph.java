package dgm.graphs.ops;

import com.google.inject.*;
import com.tinkerpop.blueprints.*;
import dgm.modules.BlueprintsSubgraphManagerModule;
import dgm.modules.LogconfModule;
import dgm.neo4j.CommonNeo4j;
import dgm.neo4j.EphemeralEmbeddedNeo4J;

import java.util.ArrayList;

public class LocalGraph {

    public static LocalGraph localNode()
    {
        final ArrayList<Module> modules = new ArrayList<Module>();
        // some defaults
        modules.add(new BlueprintsSubgraphManagerModule());
        modules.add(new CommonNeo4j());
        modules.add(new EphemeralEmbeddedNeo4J());

        modules.add(new LogconfModule());

        // the injector
        final Injector injector = com.google.inject.Guice.createInjector(modules);

        final LocalGraph lg = injector.getInstance(LocalGraph.class);

        for(Vertex v : lg.G.getVertices())
            lg.G.removeVertex(v);

        for(Edge e : lg.G.getEdges())
            lg.G.removeEdge(e);

        lg.G.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);

        return lg;
    }

    @Inject
    TransactionalGraph G;

    @Inject
    SubgraphManager sgm;
}
