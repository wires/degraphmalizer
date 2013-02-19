package streaming.requestmapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ernst Bunders
 */
public final class RequestHandlerUtil
{
    private RequestHandlerUtil() {}

    public static List<String> getGroups(String path, String regex) {
        List<String> captures = new ArrayList<String>();
        Matcher m = Pattern.compile(regex).matcher(path);

        if (m.lookingAt()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null) captures.add(m.group(i));
            }
        }
        return captures;
    }

}
