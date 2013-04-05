package dgm.modules.fsmon;

import com.google.inject.Provider;

/**
 * Adapter around a provider to cache it's value until you call invalidate
 */
class CachedProvider<T> implements Provider<T>
{
    final Provider<T> sourceProvider;

    // current value is cached here
    T cached = null;

    public CachedProvider(Provider<T> sourceProvider)
    {
        this.sourceProvider = sourceProvider;

        invalidate();
    }

    public T get()
    {
        return cached;
    }

    public boolean invalidate()
    {
        // create new cached
        final T d = sourceProvider.get();

        // return old config if loading failed
        if (d == null)
            return false;

        cached = d;
        return true;
    }
}
