package dgm.streaming.blueprints;


import dgm.streaming.command.GraphCommand;

public interface GraphCommandListener {

    void commandCreated(GraphCommand graphCommand);
}
