({
    sourceIndex: "test-merge-multiple-src",
    sourceType: "test-type",

    filter: (function(doc)
    {
	    return true;
    }),

    extract: (function(doc, subgraph)
    {
	subgraph.addEdge("label1", "test-multiple", "test-b", "virtual-1", true, {});
	subgraph.setProperty("type", "a");
    })
})
