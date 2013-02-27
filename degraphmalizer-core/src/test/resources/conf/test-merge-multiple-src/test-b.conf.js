({
    sourceIndex: "test-merge-multiple-src",
    sourceType: "test-type",

    filter: (function(doc) {
	    return true;
    }),

    extract: (function(doc, subgraph)
    {
	subgraph.addEdge("label2", "test-multiple", "test-a", "virtual-2", false, {});
	subgraph.setProperty("type", "b");
    })
})
