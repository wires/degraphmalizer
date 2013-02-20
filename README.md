![TravisStatus](https://travis-ci.org/vpro/degraphmalizer.png?branch=master)

# The Degraphmalizer!

![Pipeline](https://github.com/vpro/degraphmalizer/raw/master/pipeline.png)

The degraphmalizer is an application on top of
[Elasticsearch](http://elasticsearch.org) that can extract graph
structure from your documents and use it to add computed attributes to
your documents.

Computed attributes? Yes, the graph stores the "global" structure of your index.
Then you can add some of this global structure to your documents. For instance
you could add an "in-degree" attribute that would tell you how many incoming
links there are to a document.

You can also ask the graph questions like: give me all documents that refer to
this document. Then from those documents you can extract some information and
store that in a computed attribute. The degraphmalizer's job is to keep such
information up to date using attribute dependency tracking.

# An example

A typical example probably makes this more clear:

Suppose we have two parties, Alice and Bob. Alice has an index of authors and
Bob maintains an index of books and they refer to each other:

Alice:

	/alice/author/gibson
	{
	  "name": "William Ford Gibson",
	  "books": ["book_id_1", "book_id_2"]
	}

Bob:

	/bob/books/book_id_1
	{
	  "title": "Neuromancer"
	}

	/bob/books/book_id_2
	{
	  "title": "All Tomorrow's Parties"
	}

In order to search for authors based on their book titles, one first has to
find the id's of the books in which the words occur. Then we have to send a
second query to find the the authors that wrote those books.

In many cases it would be nicer to have an index like this

	/alice-target/author/gibson
	{
	  "name": "William Ford Gibson",
      "books": [ { "id": "book_id_1",
                   "title": "Neuromancer"
                 },
                 { "id": "book_id_2",
                   "title": "All Tomorrow's Parties" } ] 
	}

	/bob-target/books/book_id_1
	{
	  "title": "Neuromancer",
	  "authors": [ { "id": "gibson",
                     "name": "William Word Gibson" } ]
	}

	/bob-target/books/book_id_2
	{
	  "title": "All Tomorrow's Parties"
	  "authors": [ { "id": "gibson",
                     "name": "William Word Gibson" } ]
	}

So we duplicate the data of the separate indices into derived
documents which we can directly query. This process is called
"denormalization" and we are using a graph to do it, hence:
degraphmalizer :)


# Alright, Some more details plz

The degraphmalizer is configured through javascript. Here is an example
configuration that would transform Alice and Bob's index as above:

`conf/alice-target/author.conf.js`:

```javascript
({
	// we specify which documents we want to use as input for our new document
	sourceIndex: "alice",
	sourceType: "author",

	// for each document of type author, extract subgraph relation
	extract: function(doc, subgraph) {

		/* so subgraph is a tiny graph with one node corresponding
		   to the raw document in ES, (so that is id AND version).
		   the total graph is composed of all the subgraphs */

		// so we add on edge for each book this author wrote
		if(doc.books && doc.books.length)
		{
			doc.books.forEach(function(c) {
				// this constructs an edge from us to "/bob/books/c"
				// if that ID doesn't exist, a node is created for it.
				subgraph.addEdge("wrote_book", "bob", "books", c, false, {});
			})
		}
	},
	
	// we now define some walks
	walks: [
		{
			/* there are two options here: forward or backward
			   forward follows edges from tail to head, and vv.
			   
			   In the future you can define your own walks here,
			   using a DSL that automatically gives reverse walk
			*/ 
			direction: "forward",

			/* for each walk, we can define a number of properties
			   that need to be computed based on the walk */
			properties: {
				"books": {
				
					// this function is given a graph or tree
					// of documents from which it should compute
					// the property value
					reduce: function(doc_tree)
					{
						// return a flat list of dicts with id and title keys
						return bfs_walk(doc_tree).map(function(book) {
							id: book['id']
							title: book['title']
						})
					}
				}
			}
		}]
	}
})
```

# The rule that marks documents as 'dirty'

The rule: Suppose document `x` changes. We now want to find all
documents affected by this change. Suppose we have a property
depending on a forward walk, then all documents in a backward walk
starting at `x` must have this attribute recomputed.  (Because if I
would start a forward walk at any of these document I would eventually
hit `x`).

So this is why we need reversible graph walks.

# The pipeline

We receive a document, then

- Extract subgraph structure from the documents
  - We now want to find all document which are affected by this change:
  - Perform all defined walks on the graph, in reverse
	- Every node in these walk is marked as dirty
	  - For this node, perform the walk
	  - Retrieve all documents in the walk from ES
	  - Then, for every property defined on this walk,
		- Compute `reduce` of the walk
		- Update the document with the new attribute value


We can be quite smart about which documents to fetch first etc, but we
are not doing this ATM. patches welcome!

# The graph db

The graph should be a DAG, should you have a cycle then this will
cause a walk hitting the cycle to loop and explode. Boom.
Currently there is no cycle detection, so be careful.

You can pick any graph database supported by Blueprints. Currently
this runs on an embedded Neo4j graph.

The graph is a property graph model. That means that to each node or
label a set of key/value pairs is associated. Furthermore to each edge
we associate a label. Nodes are unique in the graph for `(id,version)`.
Edges are unique for `(head-id, head-version, label, tail-id, tail-verion)`.

It is up to you what information you store in the graph. You might
want to use this to restrict the graph walk. At the moment it doesn't
really matter much, as we just walk the entire forward or backward
tree from a node.

# The ES plugin

- Push configuration to `/_degraphmalize/`
- Watch every "index" request
- Perform degraphmalizing on one machine
- Replicate the graph to some other machines

You can configure the Degraphmalizer plugin using the following settings in `elasticsearch.yml`:

- `plugin.degraphmalizer.DegraphmalizerPlugin.degraphmalizerScheme`: URI scheme used to access the Degraphmalizer, either `http` or `https` (default: `http`)
- `plugin.degraphmalizer.DegraphmalizerPlugin.degraphmalizerHost`: Hostname used to access the Degraphmalizer (default: `localhost`)
- `plugin.degraphmalizer.DegraphmalizerPlugin.degraphmalizerPort`: Port used to access the Degraphmalizer (default: `9821`)
- `plugin.degraphmalizer.DegraphmalizerPlugin.delayOnFailureInMillis`: Delay in milliseconds before retrying failed requests to the Degraphmalizer (default: `10000`)
- `plugin.degraphmalizer.DegraphmalizerPlugin.queueLimit`: Number of updates to queue in memory per index before spooling to disk (default: `100000`)
- `plugin.degraphmalizer.DegraphmalizerPlugin.logPath`: Path for error logs and overflow spool files (default: `/export/elasticsearch/degraphmalizer`)
- `plugin.degraphmalizer.DegraphmalizerPlugin.maxRetries`: Number of times to retry sending an update to the Degraphmalizer before considering it failed (default: `10`)
