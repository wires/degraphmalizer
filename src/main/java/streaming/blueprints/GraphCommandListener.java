package streaming.blueprints;


import streaming.command.GraphCommand;

public interface GraphCommandListener {

    public void commandCreated(GraphCommand graphCommand);
}
