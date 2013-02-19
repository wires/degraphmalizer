package streaming.requestmapper;

import org.junit.Test;
import streaming.requestmapper.handlers.CreateStreamRequestHandler;

import static org.junit.Assert.*;

import java.util.List;

/**
 * @author Ernst Bunders
 */
public class RequestHandlerUtilTest {

    @Test
    public void testCreateStreamUrlPatternWithTwoValues() throws Exception {
        List<String> result = RequestHandlerUtil.getGroups("/createStream/1/abc", CreateStreamRequestHandler.PATH_REGEX);
        assertEquals("two captures expected", 2, result.size());
        assertEquals("First is '1'", "1", result.get(0));
        assertEquals("Second is 'abc'", "abc", result.get(1));
    }

    @Test
    public void testCreateStreamUrlPatternWithOneValues() throws Exception {
        List<String> result = RequestHandlerUtil.getGroups("/createStream/2", CreateStreamRequestHandler.PATH_REGEX);
        assertEquals("one captures expected", 1, result.size());
        assertEquals("First is '2'", "2", result.get(0));

    }
}