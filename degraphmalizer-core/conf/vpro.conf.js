({

	filter: function(doc) {
		return true;
	},

	extract: function(doc, subgraph) {
        for(var c in (doc.poms_urn || []))
			subgraph.add_edge_to("has_child", c);

	},

    "walks": {
        "forward":{
            "direction": "IN",
            "properties": {
                "nodes-out": {
                    nested: true,
                    reduce: function(doc_tree) {
                        return doc_tree;
                    }
                }
            }
        }

    }
})