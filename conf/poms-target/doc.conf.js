({
    sourceIndex: "poms",
    sourceType: "poms",

    filter: (function(doc) {
        return true;
    }),

    // void addEdge(String label,
    //          String index, String type, String id,
    //          boolean inwards,
    //          Map<String, Object> properties) throws IOException
    // void setProperty(String key, Object value) throws IOException

    extract: (function(doc, subgraph)
    {
	// _.each(doc.episodeOf || [], function(p) { ... })

	// safely map a function over an array
	var allOf = function(elt, fn) {
	    if (elt && elt.length) {
		elt.forEach(fn)
	    }
	};

        // for each episodeOf, add an edge (us) -- episodeOf --> urn
	allOf(doc.episodeOf, function(program) {
	    subgraph.addEdge("episodeOf", "poms", "poms", program['reference'], false, {});
	});

	allOf(doc.descendantOf, function(program) {
	    subgraph.addEdge("descendantOf", "poms", "poms", program['urnRef'], false, {});
	});

	// link from 'mid' node (to which subtitles link)
	if (doc.mid) {
	    subgraph.addEdge("mid", "subtitles-source", "subtitle", doc.mid, true, {});
	}

    }),

    "walks":{

	"backward":{
	    "direction": "OUT",
	    "properties": {

		"nodes-in": {
		    nested: true,
		    reduce: function(doc_tree) {
			    return doc_tree;
		    }
		},

		"teletekst-data": {
		    nested: true,
		    reduce: function(doc_tree) {

			var data = "";
			(doc_tree._children || []).map( function (c)  {
			    if (c.text != null) {
				data += c.text + " ";
			    }
			});

			return data;
		    }
		}
	    }
	},

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
