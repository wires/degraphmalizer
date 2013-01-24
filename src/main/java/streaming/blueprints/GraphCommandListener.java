package streaming.blueprints;


import streaming.command.GraphCommand;

public interface GraphCommandListener {

    void commandCreated(GraphCommand graphCommand);
}
