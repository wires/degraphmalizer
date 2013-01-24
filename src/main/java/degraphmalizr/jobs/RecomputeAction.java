package degraphmalizr.jobs;

import configuration.TypeConfig;

import com.tinkerpop.blueprints.Vertex;

public class RecomputeAction
{
	public final Vertex root;
	public final TypeConfig typeConfig;
	public final DegraphmalizeAction parent;
	
	/**
	 * Indicate that the document <i>root</i> has to be
	 * recomputed because a parent or child node of <i>d</i> has changed.
     *
     * @param parent May be null
	 */
	public RecomputeAction(DegraphmalizeAction parent, TypeConfig config, Vertex root)
	{
		this.parent = parent;
		this.root = root;
		this.typeConfig = config;
	}
}