({
    targetIndex: "koekje",
    targetType: "pretzel",

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
        // for each episodeOf, add an edge (us) -- episodeOf --> urn
        if (doc.episodeOf && doc.episodeOf.length)
        {
            doc.episodeOf.forEach(function(program){
                // add edge with some properties
                subgraph.addEdge("episodeOf", "poms", "doc", program['reference'], false,
                    {
                        position: program['position'],
                        added: program['added']
                    });
            });
        }


    }),

    "walks":{
        "backward":{
            "direction": "OUT",
            "properties": {
                "nodes-pointing-to-us": {
                    nested: true,
                    reduce: function(doc_tree)
                    {
                        return doc_tree;
                    }
                }
            }
        },
        "forward":{
            "direction": "IN",
            "properties": {
                "nodes-we-are-pointing-to": {
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