({
    sourceIndex: "poms",
    sourceType: "poms",

    extract: (function(doc,subgraph) {
        if (doc.urn && doc.urn.match("^urn:vpro:media:segment:\\d+")) {
            if (doc.urnRef) {
                subgraph.addEdge("segmentOf", "poms", "poms", doc.urnRef, false, {});
            }
        }
    }),

    filter: (function(doc) {
        if (doc.urn && !doc.urn.match("^urn:vpro:media:segment:\\d+")) {
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
            if (doc[key] !== undefined) {
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
        ['tags', 'genres', 'broadcasters', 'duration', 'start'].forEach(function( field ){
            if ( doc[field] !== undefined ) {
                result[field] = doc[field];
            }
        });

        result.title = getBestValue(doc, 'titles', 'MAIN');
        result.subTitle = getBestValue(doc, 'titles', 'SUB');
        result.description = getBestValue(doc, 'descriptions', 'MAIN');

        result.segmentId = doc.urn;

        return result;
    }),

    walks: {
        "program-info": {
            "direction": "OUT",
            "properties": {
                "program": {
                    reduce: function(doc_tree) {
                        var getBestValue = function(doc, key, type) {
                            if (doc[key] !== undefined ) {
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

                        var programInfo = {};
                        if (doc_tree._children && doc_tree._children.length > 0) {
                            var child = doc_tree._children[0];
                            if (child._value && child._value.exists) {
                                var program = child._value.value;
                                programInfo.title = getBestValue(program, 'titles', 'MAIN');
                                programInfo.subTitle = getBestValue(program, 'titles', 'SUB');
                                programInfo.id = program.urn;
                                programInfo.duration = program.duration;
                                programInfo.publishDate = getPublishDate(program);
                            }
                        }

                        return programInfo;
                    }
                },
                "publishDate": {
                    reduce: function(doc_tree) {
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

                        if (doc_tree._children && doc_tree._children.length > 0) {
                            var child = doc_tree._children[0];
                            if (child._value && child._value.exists) {
                                var program = child._value.value;
                                return getPublishDate(program);
                            }
                        }
                        return null;
                    }
                }
            }
        }
    }
})