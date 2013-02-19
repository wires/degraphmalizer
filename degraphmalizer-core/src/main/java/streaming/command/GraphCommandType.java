package streaming.command;

public enum GraphCommandType {

    AddNode, AddEdge, ChangeNode, ChangeEdge, DeleteNode, DeleteEdge;

    @Override
    public String toString() {
        switch (this) {
            case AddNode:
                return "an";
            case AddEdge:
                return "ae";
            case ChangeNode:
                return "cn";
            case ChangeEdge:
                return "ce";
            case DeleteNode:
                return "dn";
            default:
                return "de";
        }
    }
}
