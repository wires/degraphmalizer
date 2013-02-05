({
    sourceIndex: "test-index",
    sourceType: "test-type",

	filter: (function(doc) {
		return true;
	}),

// void addEdge(String label, String index, String type, String id, boolean inwards, Map<String, Object> properties) throws IOException
// void setProperty(String key, Object value) throws IOException

	extract: (function(doc, subgraph)
	{
	    if (doc.children && doc.children.length)
	    {
	       doc.children.forEach(function(n){
	            subgraph.addEdge("child", "test-index", "test-type", n, true, {"test": [1,3,5,7]});
	       });


	    }

        subgraph.setProperty("test", [1,2,3]);
	}),

    "walks":{
        "forward":{
            "direction": "IN",
            "properties": {
                "nodes-in": {
                    nested: true,
                    reduce: (function(doc_tree) {
                        return {"full_tree": doc_tree, "walk_direction": "inwards"};
                    })
                }
            }
        },
        "backward":{
            "direction": "OUT",
            "properties": {
                "nodes-out": {
                    nested: true,
                    reduce: (function(doc_tree) {
                        return {"full_tree": doc_tree, "walk_direction": "outwards"};
                    })
                }
            }
        }
    }
})