package streaming.codec;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: ernst
 * Date: 11/4/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class AddCRLFDecoder extends OneToOneEncoder {

    public static final byte[] BYTES = new byte[]{10, 13};

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof ChannelBuffer)) return msg;
        return wrappedBuffer(
                (ChannelBuffer) msg,
                wrappedBuffer(BYTES)
        );
    }
}
