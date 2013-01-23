package degraphmalizr;

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

    public ID tail()
    {
        return tail;
    }

    public String label()
    {
        return label;
    }

    public ID head()
    {
        return head;
    }

    public String toString()
    {
        return tail.toString() + " -- " + label + " --> " + head.toString();
    }
}
