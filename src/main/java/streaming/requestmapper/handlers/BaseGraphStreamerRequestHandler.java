package streaming.requestmapper.handlers;


import com.google.common.base.Optional;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import streaming.requestmapper.ChannelContext;
import streaming.requestmapper.HttpRequestMapper;
import streaming.requestmapper.RequestHandlerException;
import streaming.service.GraphUnfoldingService;

import java.util.List;

/**
 * @author Ernst Bunders
 */
public abstract class BaseGraphStreamerRequestHandler implements HttpRequestMapper.RequestHandler {
    private final GraphUnfoldingService graphStreamingService;
    private final ChannelContext channelContext;

    public BaseGraphStreamerRequestHandler(ChannelContext channelContext, GraphUnfoldingService graphStreamingService) {
        this.channelContext = channelContext;
        this.graphStreamingService = graphStreamingService;
    }

    protected final Optional<String> getParamOption(List<String> params, int index) {
        if (params.size() >= (index + 1)) return Optional.of(params.get(index));
        return Optional.absent();
    }

    protected final String getParamOrFail(List<String> params, int index, String errorMsg) throws RequestHandlerException {
        if (params.size() <= index) throw new RequestHandlerException(errorMsg, HttpResponseStatus.BAD_REQUEST);
        return params.get(index);
    }

    final GraphUnfoldingService getGraphStreamingService()
    {
        return graphStreamingService;
    }

    final ChannelContext getChannelContext()
    {
        return channelContext;
    }
}
