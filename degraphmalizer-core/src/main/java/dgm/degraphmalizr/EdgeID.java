package dgm.degraphmalizr;

public class EdgeID
{
    private final String label;

    private final ID tail;
    private final ID head;

    public EdgeID(ID tail, String label, ID head)
    {
        this.tail = tail;
        this.label = label;
        this.head = head;
    }

    public final ID tail()
    {
        return tail;
    }

    public final String label()
    {
        return label;
    }

    public final ID head()
    {
        return head;
    }

    @Override
    public final String toString()
    {
        return tail.toString() + " -- " + label + " --> " + head.toString();
    }
}
