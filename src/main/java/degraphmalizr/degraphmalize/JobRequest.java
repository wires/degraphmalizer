package degraphmalizr.degraphmalize;

import degraphmalizr.ID;

public class JobRequest {
    private final DegraphmalizeActionType actionType;
    private final ID id;

    public JobRequest(DegraphmalizeActionType actionType, ID id) {
        this.actionType = actionType;
        this.id = id;
    }

    public DegraphmalizeActionType actionType() {
        return actionType;
    }

    public ID id() {
        return id;
    }
}
