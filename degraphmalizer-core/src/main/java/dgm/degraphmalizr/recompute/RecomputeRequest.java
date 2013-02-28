package dgm.degraphmalizr.recompute;

import dgm.configuration.TypeConfig;
import dgm.degraphmalizr.VID;

public class RecomputeRequest
{
	public final VID root;
	public final TypeConfig config;

	/**
	 * Indicate that the document <i>root</i> has to be
	 * recomputed because a parent or child node of <i>d</i> has changed.
     */
	public RecomputeRequest(VID root, TypeConfig config)
	{
		this.root = root;
		this.config = config;
	}
}