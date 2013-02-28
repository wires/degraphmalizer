package dgm.streaming.talker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: ernst
 * Date: 11/3/12
 * Time: 4:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class GraphTalker extends Talker<String> {
    private BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/graph.txt")));

    public GraphTalker(SomeOne<String> someOne) {
        super(someOne);
    }

    @Override
    protected String nextMessage() {
        try {
            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }
}
