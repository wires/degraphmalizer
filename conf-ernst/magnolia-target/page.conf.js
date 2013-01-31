({
    "sourceIndex": "magnolia",
    "sourceType": "page",

    "transform": (function(doc){
        var d = {};
        d.title = doc.title;
        d.summary=doc.summary;
        d.url = doc.deeplink;
        d.eentje = "vijf";
        return d;
    }),

    "filter": (function(doc){
        return true;
    })

    ,"walks":{

        "backward":{
            "direction": "IN",
            "properties": {
                "nodes-in": {
                    nested: true,
                    reduce: (function(doc_tree) {
                        return doc_tree;
                    })
                }
            }
        },


        "forward":{
            "direction": "OUT",
            "properties": {
                "nodes-out": {
                    nested: true,
                    reduce: (function(doc_tree) {
                        return doc_tree;
                    })
                }
            }
        }
    }

})