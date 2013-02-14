({
    sourceIndex: "poms",
    sourceType: "poms",

    extract: (function(doc,subgraph) {
        if (doc.urn && doc.urn.match("^urn:vpro:media:program:\\d+")) {
            if (doc.episodeOf) {
                doc.episodeOf.forEach(function(group) {
                    subgraph.addEdge("episodeOf", "poms", "poms", group.urnRef, false, {});
                });
            }
        }
    }),

    filter: (function(doc) {
        if (doc.urn && !doc.urn.match("^urn:vpro:media:program:\\d+")) {
            return false;
        }

        if (!doc.locations || doc.locations.length == 0) {
            return false;
        }

        if (doc.descendantOf) {
            for (var key in doc.descendantOf) {
                var parent = doc.descendantOf[key];
                if (parent.urnRef && parent.urnRef == "urn:vpro:media:group:11887617") {
                    return true;
                }
            }
        }

        return false;
    }),

    transform: (function(doc) {
        var getBestValue = function(doc, key, type) {
            if (doc[key]) {
                for (var idx in doc[key]) {
                   var value = doc[key][idx];
                   if (value.owner && value.owner == 'BROADCASTER' && value.type && value.type == type) {
                       return value.value;
                   }
                }

                for (var idx in doc[key]) {
                    var value = doc[key][idx];
                    if (value.type && value.type == type) {
                        return value.value;
                    }
                }

                for (var idx in doc[key]) {
                    var value = doc[key][idx];
                    return value.value;
                }
            }
            return '';
        };

        var getPublishDate = function(doc) {
            if (doc.scheduleEvents && doc.scheduleEvents.length > 0) {
                return doc.scheduleEvents[0].start;
            } else if (doc.publishStart) {
                return doc.publishStart;
            } else if (doc.creationDate) {
                return doc.creationDate;
            }
            return null;
        };

        var result = {};

        // Copy default fields
        ['tags', 'genres', 'broadcasters', 'duration'].forEach(function(field){
            if (doc[field]) {
                result[field] = doc[field];
            }
        });

        result.title = getBestValue(doc, 'titles', 'MAIN');
        result.subTitle = getBestValue(doc, 'titles', 'SUB');
        result.description = getBestValue(doc, 'descriptions', 'MAIN');

        result.programId = doc.urn;
        result.publishDate = getPublishDate(doc);

        return result;
    }),

    walks: {
        "published-series-titles": {
            "direction": "OUT",
            "properties": {
                "programTitles": {
                    reduce: function(doc_tree) {
                        var getBestValue = function(doc, key, type) {
                            if (doc[key]) {
                                for (var idx in doc[key]) {
                                    var value = doc[key][idx];
                                    if (value.owner && value.owner == 'BROADCASTER' && value.type && value.type == type) {
                                        return value.value;
                                    }
                                }

                                for (var idx in doc[key]) {
                                    var value = doc[key][idx];
                                    if (value.type && value.type == type) {
                                        return value.value;
                                    }
                                }

                                for (var idx in doc[key]) {
                                    var value = doc[key][idx];
                                    return value.value;
                                }
                            }
                            return '';
                        };

                        var titles = [];
                        if (doc_tree._children) {
                            for (var idx in doc_tree._children) {
                                var child = doc_tree._children[idx];
                                if (child._value && child._value.exists) {
                                    var group = child._value.value;
                                    if (group.type && group.type == 'SERIES' && group.workflow && group.workflow == 'PUBLISHED') {
                                        var title = getBestValue(group, 'titles', 'MAIN');
                                        titles.push(title);
                                    }
                                }
                            }
                        }
                        return titles;
                    }
                }
            }
        }
    }
})