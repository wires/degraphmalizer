package degraphmalizr.recompute;

/**
 * Recompute documents
 */
public interface Recomputer
{
    RecomputeResult recompute(final RecomputeRequest request, RecomputeCallback callback);
}