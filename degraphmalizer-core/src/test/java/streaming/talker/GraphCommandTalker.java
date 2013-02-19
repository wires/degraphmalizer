package streaming.talker;

import streaming.command.GraphCommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static streaming.command.GraphCommandBuilder.*;

public class GraphCommandTalker extends Talker<GraphCommand> {
    private int counter = 0;
    private Iterator<GraphCommand> commandsIterator;

    public GraphCommandTalker(SomeOne<GraphCommand> someOne) {
        super(someOne);
        List<GraphCommand> commands = new ArrayList<GraphCommand>();

        //create node
        commands.add(addNodeCommand(node("aap", 5).set("label", "Aap")).build());
        commands.add(addNodeCommand(node("noot", 5).set("label", "Noot")).build());
        commands.add(addNodeCommand(node("mies", 5).set("label", "Mies")).build());

        //create edge
        commands.add(addEdgeCommand(edge("edgy", "aap", "noot", false, 5)).build());
        commands.add(addEdgeCommand(edge("edgism", "aap", "mies", true)).build());


        // update node / edge
        commands.add(updateNodeCommand(node("aap").set("label", "Aapje")).build());
        commands.add(updateEdgeCommand(node("edgy").set("label", "Edgy!!")).build());

        // delete node / edge
        commands.add(deleteEdgeCommand("edgism").build());
        commands.add(deleteNodeCommand("mies").build());

        commandsIterator = commands.iterator();
    }

    @Override
    protected GraphCommand nextMessage() {
        return commandsIterator.hasNext() ? commandsIterator.next() : null;
    }
}
