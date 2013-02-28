package dgm;

import java.io.Serializable;

public class ID implements Serializable
{
    static final long serialVersionUID = 1;

    private final String index;
    private final String type;
    private final String id;
    private final long version;

    public ID(String index, String type, String id, long version)
    {
        this.index = index;
        this.type = type;
        this.id = id;
        this.version = version;
    }

    public final String index()
    {
        return index;
    }

    public final String type()
    {
        return type;
    }

    public final String id()
    {
        return id;
    }

    public final long version()
    {
        return version;
    }

    public final boolean isSymbolic()
    {
        return version == 0;
    }

    public ID type(String newType)
    {
        return new ID(index, newType, id, version);
    }

    public ID index(String newIndex)
    {
        return new ID(newIndex, type, id, version);
    }

    public ID id(String newId)
    {
        return new ID(index, type, newId, version);
    }

    public ID version(long newVersion)
    {
        return new ID(index, type, id, newVersion);
    }

    @Override
    public final String toString()
    {
        return "(" + index + "," + type + "," + id + "," + version + ")";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ID id1 = (ID) o;

        if (version != id1.version) return false;
        if (id != null ? !id.equals(id1.id) : id1.id != null) return false;
        if (index != null ? !index.equals(id1.index) : id1.index != null) return false;
        if (type != null ? !type.equals(id1.type) : id1.type != null) return false;

        return true;
    }

    @Override
    public final int hashCode() {
        int result = index != null ? index.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (int) (version ^ (version >>> 32));
        return result;
    }
}