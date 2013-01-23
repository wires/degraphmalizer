// Create a subgraph for the specific function
Subgraph = function(sg)
{
    this.sg = sg
    // sg : Subgraph

    /*
    void insertEdge(String tail, String type, String head);
    void addEdge(String tail, String type, String head, String property, JsonNode value);
    void addNode(String id, String property, JsonNode value);
    */
};

Subgraph.prototype =
{

	constructor: Subgraph,

	// helper function to edge edges to graph
	_add_edge: function(head, label, tail, properties)
	{
		var properties = properties || {}

		// we have no properties
		if(!properties)
		{
			this.sg.insertEdge(tail, label, head)
			return
		}

		// create the edge and add all the properties
		for (var key in properties)
			this.sg.addEdge(tail, label, head, key, properties[key]);
	},

	add_edge_from: function(label, other, properties)
	{
		this._add_edge(this.id, label, other, properties)
	},

	add_edge_to: function(label, other, properties)
	{
		this._add_edge(other, label, this.id, properties)
	},
	
	add_property: function(key, value)
	{
		_graph.add_node_property(this.id, key, value);
	},

	add_properties: function(properties)
	{
		for(var key in properties)
			_graph.add_node_property(this.id, key, properties[key]);
	}
};
