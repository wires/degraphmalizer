package dgm.graphs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.*;
import dgm.*;
import dgm.exceptions.DegraphmalizerException;
import org.neo4j.helpers.collection.Iterables;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.*;

import static dgm.GraphUtilities.*;
import static org.fest.assertions.Assertions.assertThat;

public class SubgraphManagerTest
{
    final Random random = new Random(System.currentTimeMillis());
    LocalGraph lg;
    final ObjectMapper om = new ObjectMapper();

    @BeforeMethod
    public void clearGraph()
    {
        lg = LocalGraph.localNode();
    }

    @AfterMethod
    public void shutdownGraph()
    {
        lg.G.shutdown();
    }

    /**
     * It should not be possible to create an edge that has the same id for head and tail
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    void testCommitOrUpdateEdgeWithHeadEqualToTailFails() throws DegraphmalizerException
    {
        final MutableSubgraph sg = new MutableSubgraph();
        final ID id = randomVersionedID();
        sg.beginEdge("test", getSymbolicID(id), Subgraph.Direction.INWARDS);
        lg.sgm.commitSubgraph(id, sg);
    }

    /**
     * If you commitAndFindCentralVertex a subgraph, All the mandatory properties should be created, plus
     * the extra properties that you set on the subgraph.
     *
     * @throws Exception
     */
    @Test
    void testCommitSubgraphCreateVertex() throws Exception
    {
        // commit subgraph and verify
        final Props p = new Props("[1,2,3]", "{\"a\":2}", "3", "\"abc\"", "{}");
        commitSubgraphAndVerifyProperties(p, randomVersionedID());
    }

    /**
     * One should not be allowed to commit a subgraph to a symbolic ID (ie. version == 0)
     *
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    void testVersionZeroNotAllowed()
    {
        final ID symbolicID = randomSymbolicID();
        lg.sgm.commitSubgraph(symbolicID, Subgraphs.EMPTY_SUBGRAPH);
    }

    /**
     * Thou shall not create edges referring to other vertices by version!
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    void testNotSymbolicEdgeAllowed() throws DegraphmalizerException
    {
        final MutableSubgraph sg = new MutableSubgraph();
        sg.beginEdge("label", randomVersionedID(), Subgraph.Direction.INWARDS);
        lg.sgm.commitSubgraph(randomVersionedID(), sg);
    }

    /**
     * if you create a subgraph with an id for which a symbolic node already exists, it
     * should claim that node.
     * @throws Exception
     */
    @Test
    void testCommitSubgraphWithEdgeClaimsExistingSymbolicVertexForEdge() throws Exception
    {
        // first create some node in the graph
        final ID id0 = randomVersionedID();
        final Props p0 = new Props("[1,2,3]", "{\"a\":2}", "3", "\"abc\"", "{}");
        commitSubgraphAndVerifyProperties(p0, id0);

        // now we create a subgraph referring the node above
        final ID id1 = randomVersionedID();
        final MutableSubgraph sg1 = new MutableSubgraph();
        final Props p1 = new Props("\"foo\"");
        p1.modifySubgraph(sg1);
        sg1.beginEdge("label", getSymbolicID(id0), Subgraph.Direction.INWARDS);
        lg.sgm.commitSubgraph(id1, sg1);

        // there should now be two nodes, and one edge
        assertThat(lg.G.getVertices()).hasSize(2);
        assertThat(lg.G.getEdges()).hasSize(1);

        final Vertex v0 = resolveVertex(om, lg.G, id0);
        final Vertex v1 = resolveVertex(om, lg.G, id1);

        assertThat(getID(om, v0)).isEqualTo(id0);
        assertThat(getID(om, v1)).isEqualTo(id1);
        p0.assertOK(v0);
        p1.assertOK(v1);

        final EdgeID edgeID = new EdgeID(id0, "label", id1);
        final Edge e = findEdge(om, lg.G, edgeID);

        assertThat(e).isNotNull();
        assertThat(e.getVertex(Direction.IN)).isEqualTo(v1);
        assertThat(e.getVertex(Direction.OUT)).isEqualTo(v0);
    }

    /**
     * If you create a subgraph for which a symbolic vertex exists, it should claim that
     * vertex, like there is no tomorrow!
     * Also should it adopt the edges from that id
     */
    @Test
    void testCommitSubgraphClaimsExistingSymbolicVertex() throws DegraphmalizerException
    {
        //first create a graph with two vertices and an edge. One vertex is symbolic, the other is not.
        final ID id1 = new ID("a", "b", "c1", 0);
        final ID id2 = new ID("a", "b", "c2", 2);
        addVertexWithId(id1, true);
        addVertexWithId(id2, false);
        createEdge(om, lg.G, new EdgeID(id1, "edge", id2));

        //now create a subgraph for id1, but with a version
        final ID id1a = new ID("a", "b", "c1", 1);
        Vertex v1 = commitAndFindCentralVertex(id1a);

        //now assert we still have the edge
        assertThat(Iterables.toList(v1.getEdges(Direction.BOTH)).size()).isEqualTo(1);
        assertThat(v1.getEdges(Direction.BOTH).iterator().next().getId().equals(id2));
    }

    @Test
    void testCommitSubgraphWithExistingIdThrows()
    {
        //TODO: implement
    }

    /**
     * if you create a subgraph with edges then the central vertex, the edges and symbolic vertices should be created
     * in the graph.
     *
     * @throws Exception
     */
    @Test
    void testCommitSubgraphCreateEdgesAndCreatesSymbolicVertices() throws Exception
    {
        final ID sourceId = randomVersionedID();
        final ID targetId = randomSymbolicID();

        // create a subgraph that has an edge. the other node of the edge doesn't exist yet.
        final MutableSubgraph sg = new MutableSubgraph();

        // create an edge: sourceId ---> targetId
        final Props p = new Props("true", "\"twee\"");

        final MutableSubgraph.Edge e = sg.beginEdge("edge1", targetId, Subgraph.Direction.OUTWARDS);
        for(Map.Entry<String,JsonNode> pe : p.properties.entrySet())
            e.property(pe.getKey(), pe.getValue());

        final Vertex v = commitAndFindCentralVertex(sg, sourceId);

        assertThat(lg.G.getEdges()).hasSize(1);
        assertThat(lg.G.getVertices()).hasSize(2);

        // test if the edge is created, and the other node as well. It should be a symbolic node.
        Edge edge = v.getEdges(Direction.OUT).iterator().next();

        //edge -> IN == v2 :: edge -> OUT == v1
        final Vertex head = edge.getVertex(Direction.IN);

        assertThat(getID(om, head).version()).isZero();
        assertThat(getID(om, head)).isEqualTo(targetId);
        assertThat(getID(om, v)).isEqualTo(sourceId);
    }

    /**
     * If you create a subgraph with edges to symbolic vertices that are already in the graph, these should be
     * used to create the edges to.
     * @throws Exception
     */
    @Test
    void testCommitSubgraphCreateEdgesUsesExistingSymbolicVertices() throws Exception
    {
        //first create an existing virtual node
        ID  targetId = randomSymbolicID();
        addVertexWithId(targetId, true);

        //then create a subgraph, that creates an edge to this existing symbolic vertex
        final ID id = randomVersionedID();
        final MutableSubgraph sg = new MutableSubgraph();
        sg.beginEdge("edge1", targetId, Subgraph.Direction.OUTWARDS);
        lg.sgm.commitSubgraph(id, sg);
        Vertex v = commitAndFindCentralVertex(sg, id);

        //test if the edge has been created properly
        Edge edge = v.getEdges(Direction.OUT).iterator().next();
        Vertex vout = edge.getVertex(Direction.IN);
        assertThat(getID(om, vout)).isEqualTo(targetId);

        //test if exactly two vertices are created.
        assertThat(Iterables.toList(lg.G.getVertices()).size()).isEqualTo(2);
    }

    /**
     * When you create a subgraph with an edge to an existing vertex, and this vertex is not symbolic, its properties
     *should remain intact.
     * In practice you may not know what the version is for the target vertex. Perhaps the version field should be
     * ignored for 'other' vertices for edges?
     */
    @Test
    public void creatingAnEdgeToAVertexShouldLeaveItsPropertiesIntact() throws Exception
    {
        //create an existing vertex in the graph, with some properties, and a real version.
        ID targetID = new ID("a", "b", "c", 1);
        Vertex v = addVertexWithId(targetID, false);
        v.setProperty("foo", "bar");
        v.setProperty("too", "bad");

        //create a subgraph that links to this vertex, and commit it.
        final MutableSubgraph sg = new MutableSubgraph();
        sg.beginEdge("edge1", getSymbolicID(targetID), Subgraph.Direction.INWARDS);
        lg.sgm.commitSubgraph(randomVersionedID(), sg);

        //test that the vertex still has the properties.
        Vertex targetVertex = findVertex(om, lg.G, targetID);
        assertThat(targetVertex).isEqualTo(v);
        assertThat(targetVertex.getPropertyKeys().size()).isEqualTo(5);
        checkElementProperty(targetVertex, "foo", "bar");
        checkElementProperty(targetVertex, "too", "bad");
    }

    /**
     * Updating a subgraph should remove the properties of the old version that are not in the new version too.
     * @throws Exception
     */
    @Test
    void testUpdateSubgraphReplacesProperties() throws Exception
    {
        //first create a graph with a vertex for the id, with some properties. This is not a symbolic vertex, but
        //the previous version.
        final ID ID = randomVersionedID();
        Vertex v = addVertexWithId(ID, false);
        v.setProperty("foo", "bar");

        //now create a subgraph for this id, and set different properties.
        ID newID = new ID(ID.index(), ID.type(), ID.id(), ID.version()+1);
        final MutableSubgraph sg = new MutableSubgraph();
        new Props("\"een\"", "\"twee\"").modifySubgraph(sg);
        Vertex v1 = commitAndFindCentralVertex(sg, newID);

        //now check if all the old properties are gone.
        checkElementProperty(v1, "prop0", "een");
        checkElementProperty(v1, "prop1", "twee");
        assertThat(v1.getPropertyKeys().contains("foo")).isFalse();
        assertThat(getID(om, v1).version()).isEqualTo(newID.version());
    }

    /**
     * Creating a subgraph for an existing vertex with a higher version should work ok.
     * @throws DegraphmalizerException
     */
    @Test
    void testUpdateSubgraphWithHigherVersion() throws DegraphmalizerException
    {
        testUpgradeSubgraphToVersion(2, 3);
    }

    /**
     * Creating a subgraph for an existing vertex with an equal version should work ok.
     * @throws DegraphmalizerException
     */
    @Test
    void testUpdateSubgraphWithEqualVersion() throws DegraphmalizerException
    {
        testUpgradeSubgraphToVersion(2, 2);
    }

    private void testUpgradeSubgraphToVersion(long preveousVersion, long nextVersion) throws DegraphmalizerException
    {
        //first create a graph with a vertex for the id This is not a symbolic vertex, but
        //the previous version.
        final ID id = new ID("a", "b", "c", preveousVersion);
        addVertexWithId(id, false);

        //now create a subgraph for the same id, with a new version.
        //If we commit the subgraph and the version is not equal or higher, a DegraphmalizerException should be thrown
        final ID nextVersionId = new ID("a", "b", "c", nextVersion);
        lg.sgm.commitSubgraph(nextVersionId, new MutableSubgraph());
    }

    /**
     * If you update a subgraph:
     * - edges in the previous version but not in the new one should be removed
     * - target vertices of those obsolete edges that are symbolic should be removed.
     * - target vertices of those obsolete edges that are not symbolic should be maintained;
     */
    @Test
    void testUpdateSubgraphShouldRemovePreviousEdgesAndSymbolicTargetVertices() throws DegraphmalizerException
    {
        //first create a subgraph with a bunch of edges
        ID rootId = new ID("a", "b", "c0", 1);
        final ID realVertexIncoming = new ID("a", "c", "c1", 3);
        final ID realVertexOutgoing = new ID("a", "d", "c2", 3);
        final ID symbolicVertexIncoming = new ID("a", "c", "c3", 0); /* symbolic nodes can be claimed by the subgraph. */
        final ID symbolicVertexOutGoing = new ID("a", "c", "c4", 0);

        addEdgeAndVertices(rootId, realVertexOutgoing, "e1", Direction.OUT);
        addEdgeAndVertices(rootId, symbolicVertexOutGoing, "e2", Direction.OUT);
        addEdgeAndVertices(rootId, realVertexIncoming,  "e3", Direction.IN);
        addEdgeAndVertices(rootId, symbolicVertexIncoming, "e4", Direction.IN);

        lg.G.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);

        //let's check we actually have the edges.
        final Vertex v1 = findVertex(om, lg.G, rootId);
        assertThat(Iterables.toList(v1.getEdges(Direction.OUT))).hasSize(2);
        assertThat(Iterables.toList(v1.getEdges(Direction.IN))).hasSize(2);

        //and now update the subgraph to a version without edges.
        rootId = updateIDVersion(rootId, 2);
        final MutableSubgraph sg = new MutableSubgraph();
        lg.sgm.commitSubgraph(rootId, sg);

        //assert that the new subgraph has no edges,
        final Vertex v2 = findVertex(om, lg.G, rootId);
        assertThat(Iterables.toList(v2.getEdges(Direction.BOTH))).isEmpty();

        //the two symbolic vertices are gone,
        assertThat(findVertex(om, lg.G, symbolicVertexIncoming)).isNull();
        assertThat(findVertex(om, lg.G, symbolicVertexOutGoing)).isNull();

        //and the two versioned vertices are still there.
        assertThat(findVertex(om, lg.G, realVertexIncoming)).isNotNull();
        assertThat(findVertex(om, lg.G, realVertexOutgoing)).isNotNull();
    }

    @Test
    void testDeleteSubgraph() throws DegraphmalizerException
    {
        // Setup first subgraph
        final ID v0 = new ID("a", "b", "v0", 1);
        final ID v1s = new ID("a", "b", "v1", 0);
        final EdgeID e0 = new EdgeID(v0, "e0", v1s);
        final MutableSubgraph sg0 = new MutableSubgraph();
        sg0.beginEdge(e0.label(), v1s, Subgraph.Direction.OUTWARDS);
        lg.sgm.commitSubgraph(v0, sg0);

        // Setup second subgraph
        final ID v1 = new ID("a", "b", "v1", 1);
        final ID v2s = new ID("a", "b", "v2", 0);
        final EdgeID e1 = new EdgeID(v1, "e1", v2s);
        final MutableSubgraph sg1 = new MutableSubgraph();
        sg1.beginEdge(e1.label(), v2s, Subgraph.Direction.OUTWARDS);
        lg.sgm.commitSubgraph(v1, sg1);

        // Setup third subgraph
        final ID v2 = new ID("a", "b", "v2", 1);
        lg.sgm.commitSubgraph(v2, new MutableSubgraph());

        // DELETE second subgraph!
        lg.sgm.deleteSubgraph(v1);

        // Check vertices
        Vertex vertex0 = findVertex(om, lg.G, v0);
        Vertex vertex1 = findVertex(om, lg.G, v1s);
        Vertex vertex2 = findVertex(om, lg.G, v2);

        assertThat(vertex0).isNotNull();
        assertThat(vertex1).isNotNull();
        assertThat(vertex2).isNotNull();

        assertThat(GraphUtilities.isSymbolic(om, vertex0)).isFalse();
        assertThat(GraphUtilities.isSymbolic(om, vertex1)).isTrue();
        assertThat(GraphUtilities.isSymbolic(om, vertex2)).isFalse();

        // Check edges
        final Edge edge0 = findEdge(om, lg.G, e0);
        final Edge edge1 = findEdge(om, lg.G, e1);

        assertThat(edge0).isNotNull();
        assertThat(edge1).isNull();
    }

    private Vertex addVertexWithId(ID id, boolean isSymbolic)
    {
        final Vertex vertex = lg.G.addVertex(id);
        final ObjectMapper om = new ObjectMapper();
        final String identifier = JSONUtilities.toJSON(om, isSymbolic ? getSymbolicID(id) : id).toString();
        final String symbolicIdentifier = JSONUtilities.toJSON(om, getSymbolicID(id)).toString();

        vertex.setProperty(IDENTIFIER, identifier);
        vertex.setProperty(OWNER, identifier);
        vertex.setProperty(SYMBOLIC_IDENTIFER, symbolicIdentifier);
        return vertex;
    }

    /**
     * Creates an edge and also creates the vertices. The vertex id is used for the vertex owner, and the
     * graph id is used for the edges ownership
     *
     * @param graphID id of the center vertex of the subgraph
     * @param targetID id of the target Vertex
     * @param label the label for the edge
     * @param direction the edge direction.
     */
    private void addEdgeAndVertices(final ID graphID, final ID targetID, final String label, Direction direction)
    {
        for (final ID id : Arrays.asList(graphID, targetID)) {
            if (findVertex(om, lg.G, id) == null) {
                setOwner(om, createVertex(om, lg.G, id), id);
            }
        }

        if (direction == Direction.IN) {
            setOwner(om, createEdge(om, lg.G, new EdgeID(targetID, label, graphID)), graphID);
        }else{
            setOwner(om, createEdge(om, lg.G, new EdgeID(graphID, label, targetID)), graphID);
        }
    }

    private void checkElementProperty(Element elt, String key, String expectedValue)
    {
        assertThat(elt.getPropertyKeys()).contains(key);
        assertThat(elt.getProperty(key)).isEqualTo(expectedValue);
    }

    /**
     * this method:
     * - creates a subgraph with given id.
     * - puts the props on the subgraph
     * - commits the subgraph into the graph
     * - checks the properties on the created vertex.
     * @param p properties
     * @param id id of the center vertex of the subgraph
     * @return the committed subgraph
     * @throws DegraphmalizerException
     * @throws IOException
     */
    private Subgraph commitSubgraphAndVerifyProperties(Props p, ID id) throws DegraphmalizerException, IOException
    {
        final MutableSubgraph sg = new MutableSubgraph();

        p.modifySubgraph(sg);
        final Vertex center = commitAndFindCentralVertex(sg, id);
        p.assertOK(center);

        final ObjectMapper om = new ObjectMapper();
        final String identifier = JSONUtilities.toJSON(om, id).toString();
        final String symbolicidentifier = JSONUtilities.toJSON(om, getSymbolicID(id)).toString();

        checkElementProperty(center, IDENTIFIER, identifier);
        checkElementProperty(center, OWNER, identifier);
        checkElementProperty(center, SYMBOLIC_OWNER, symbolicidentifier);
        checkElementProperty(center, SYMBOLIC_IDENTIFER, symbolicidentifier);

        assertThat(center.getPropertyKeys()).hasSize(4 + p.properties.size());

        return sg;
    }

    private Vertex commitAndFindCentralVertex(Subgraph sg, ID id) throws DegraphmalizerException
    {
        lg.sgm.commitSubgraph(id, sg);
        return findVertex(om, lg.G, id);
    }

    private Vertex commitAndFindCentralVertex(ID id) throws DegraphmalizerException
    {
        return commitAndFindCentralVertex(new MutableSubgraph(), id);
    }

    private ID randomSymbolicID() { return generateRandomID(0); }

    private ID randomVersionedID() { return generateRandomID(random.nextInt(10000) +1); }

    private ID generateRandomID(long version)
    {
        return new ID(randomString(), randomString(), randomString(), version);
    }

    private String randomString()
    {
        final String s = UUID.randomUUID().toString();
        return s.substring(0, random.nextInt(s.length()));
    }

    private ID updateIDVersion(final ID id, final long version) {
        return new ID(id.index(), id.type(), id.id(), version);
    }
}

class Props
{
    final ObjectMapper om = new ObjectMapper();
    final HashMap<String,JsonNode> properties = new HashMap<String, JsonNode>();

    Props(String... propVals) throws IOException
    {
        int i = 0;
        for(String v : propVals)
            properties.put("prop" + (i++), om.readTree(v));
    }

    void modifySubgraph(MutableSubgraph sg)
    {
        for(Map.Entry<String,JsonNode> e : properties.entrySet())
            sg.property(e.getKey(), e.getValue());
    }

    void assertOK(Element v) throws IOException
    {
        for(Map.Entry<String,JsonNode> e : properties.entrySet())
        {
            final String name = e.getKey();
            final JsonNode expected = e.getValue();

            final JsonNode n = getProperty(om, v, name);
            assertThat(n.equals(expected)).isTrue();
        }
    }
}