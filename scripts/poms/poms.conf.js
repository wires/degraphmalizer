({
	filter: (function(doc) {
		return true;
	}),

	extract: (function(doc, subgraph) {
		if(doc.parent)
			subgraph.add_edge_from("parent", doc.parent);
	}),

    "walks":{
        "forward":{
            "direction": "IN",
            "properties": {
                "nodes-out": {
                    nested: true,

                    reduce: function(doc_tree)
                    {
                        return doc_tree;
                    }
                }
            }
        }
    }
})