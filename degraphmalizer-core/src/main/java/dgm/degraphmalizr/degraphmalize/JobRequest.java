package dgm.degraphmalizr.degraphmalize;

import dgm.ID;

public class JobRequest {
    private final DegraphmalizeActionType actionType;
    private final DegraphmalizeActionScope actionScope;
    private final ID id;

    public JobRequest(DegraphmalizeActionType actionType, DegraphmalizeActionScope actionScope, ID id) {
        this.actionType = actionType;
        this.actionScope = actionScope;
        this.id = id;
    }

    public DegraphmalizeActionType actionType() {
        return actionType;
    }

    public DegraphmalizeActionScope actionScope() {
        return actionScope;
    }

    public ID id() {
        return id;
    }
}
