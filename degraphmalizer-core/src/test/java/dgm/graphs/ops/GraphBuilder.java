package dgm.graphs.ops;

import dgm.degraphmalizr.EdgeID;
import dgm.degraphmalizr.ID;

import java.util.*;

/**
 *
 */
public class GraphBuilder
{
    final Random random;
    final HashMap<String, String> environment = new HashMap<String, String>();

    public GraphBuilder(int randomSeed)
    {
        random = new Random(randomSeed);
    }

    // "(a,b,c,d) -- label --> (d,e,f,g)"
    public EdgeID edge(String edgeSpec)
    {
        final String[] s = edgeSpec.trim().split(" ");
        final String tail_str = s[0];
        final String label = s[2];
        final String head_str = s[4];

        if(s[1].length() != 2 || s[3].length() != 3)
            throw new RuntimeException("Invalid specification format, '--' and '-->' are used to specify edges");

        final ID tail = parseIDStr(s[0], "Tail");
        final ID head = parseIDStr(s[4], "Head");

        return new EdgeID(tail, label, head);
    }

    // mutate the environment, place random values for the identifiers
    private ID parseIDStr(String s, String whichOne)
    {
        if(! (s.startsWith("(") && s.endsWith(")")))
            throw new RuntimeException(whichOne +
                    " vertex identifier must be enclosed within round brackets, '(' and ')'");

        // split variable
        final String[] vars = s.substring(1, s.length() - 1).split(",");

        if(vars.length != 4)
            throw new RuntimeException(whichOne +
                    " vertex identifier must have four elements, e.g. '(a,b,c,0)' or '(foo,bar,baz,123)'");


        final String index = lookupOrCreate(vars[0], environment, false);
        final String type = lookupOrCreate(vars[1], environment, false);
        final String id = lookupOrCreate(vars[2], environment, false);
        final String version = lookupOrCreate(vars[3], environment, true);

        return new ID(index, type, id, Integer.parseInt(version));
    }

    private String lookupOrCreate(String key, Map<String,String> environment, boolean number)
    {
        // the symbolic version always resolves to 0
        if(number && key.equals("0"))
            return "0";

        // create new random value, either an integer or a random string
        if(!environment.containsKey(key))
        {
            final String value = freshName(environment, number);
            environment.put(key, value);
            return value;
        }

        return environment.get(key);
    }

    private String freshName(Map<String,String> environment, boolean numeric)
    {
        final int MAX_TRIES = 32;
        for(int i = 0; i < MAX_TRIES; i++)
        {
            final String value;

            if(numeric)
                // create random positive number
                value = Integer.toString(random.nextInt(Integer.MAX_VALUE-10)+1);
            else
                // take some random word
                value = RandomIdentifierGenerator.randomWord(random.nextInt(Integer.MAX_VALUE));

            // value already exists in our environment
            if(environment.values().contains(value))
                continue;

            return value;
        }

        throw new RuntimeException("Couldn't find a fresh value after " + MAX_TRIES + " tries");
    }
}
