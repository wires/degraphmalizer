package degraphmalizr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.codehaus.jackson.annotate.JsonIgnore;

@JsonIgnoreProperties("symbolic")
@JsonPropertyOrder({ "idx", "t", "id", "v" })
public class ID
{
    @JsonProperty("idx")
    final String index;

    @JsonProperty("t")
    final String type;

    @JsonProperty("id")
    final String id;

    @JsonProperty("v")
    final long version;

    @JsonCreator
    public ID(@JsonProperty("idx") String index, @JsonProperty("t") String type, @JsonProperty("id") String id, @JsonProperty("v") long version)
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

    @JsonIgnore
    public final boolean isSymbolic()
    {
        return version == 0;
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