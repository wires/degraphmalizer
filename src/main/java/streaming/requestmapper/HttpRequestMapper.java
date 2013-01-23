package streaming.requestmapper;

import com.google.common.base.Optional;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ernst Bunders
 */
public final class HttpRequestMapper {
    private final List<HttpRequestMapping> requestMappings = new ArrayList<HttpRequestMapping>();
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestMapper.class);


    public HttpRequestMapper addMapping(RequestHandler requestHandler, HttpMethod... methods) {
        requestMappings.add(new HttpRequestMapping(Arrays.asList(methods), requestHandler));
        return this;
    }

    public void handleRequest(HttpRequest httpRequest, Channel channel) throws RequestHandlerException {
        Optional<RequestHandler> handlerOption = selectHandler(httpRequest);
        if (handlerOption.isPresent()) {
            handlerOption.get().handleRequest(httpRequest, channel);
        } else {
            LOGGER.error("No request handler found for request: " + httpRequest);
        }
    }

    private Optional<RequestHandler> selectHandler(final HttpRequest httpRequest) {
        List<RequestHandler> possibleHandlers = new ArrayList<RequestHandler>();
        for (HttpRequestMapping mapping : requestMappings) {
            if (mapping.methods.contains(httpRequest.getMethod()) && mapping.pathMatcher.reset(httpRequest.getUri()).find()) {
                possibleHandlers.add(mapping.handler);
            }
        }
        switch (possibleHandlers.size()) {
            case 0:
                return Optional.absent();
            case 1:
                return Optional.of(possibleHandlers.get(0));
            default:
                throw new IllegalStateException("More than one possible handler was found for request query string: " + httpRequest.getUri());
        }
    }

    private static final class HttpRequestMapping {
        final List<HttpMethod> methods;
        final Matcher pathMatcher;
        final RequestHandler handler;

        private HttpRequestMapping(List<HttpMethod> methods, RequestHandler handler) {
            this.methods = methods;
            this.handler = handler;
            Pattern pattern = Pattern.compile(handler.getPathMatchingExpression());
            pathMatcher = pattern.matcher("").reset(); /* just create the matcher for reuse: should this be thread safe???*/
        }
    }

    public interface RequestHandler {
        public void handleRequest(HttpRequest request, Channel channel) throws RequestHandlerException;

        /**
         * Returns the regular expression string that will be used to match the request path with.
         */
        public String getPathMatchingExpression();
    }
}
