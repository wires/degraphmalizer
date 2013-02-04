package degraphmalizr.jobs;

import configuration.TypeConfig;
import degraphmalizr.VID;

public class RecomputeAction
{
	public final VID root;
	public final TypeConfig typeConfig;

	/**
	 * Indicate that the document <i>root</i> has to be
	 * recomputed because a parent or child node of <i>d</i> has changed.
     */
	public RecomputeAction(VID root, TypeConfig config)
	{
		this.root = root;
		this.typeConfig = config;
	}
}