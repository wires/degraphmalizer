package streaming.codec;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

public class AddCRLFDecoder extends OneToOneEncoder {

    private static final byte[] BYTES = new byte[]{10, 13};

    @Override
    protected final Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) {
        if (!(msg instanceof ChannelBuffer)) return msg;
        return wrappedBuffer(
                (ChannelBuffer) msg,
                wrappedBuffer(BYTES)
        );
    }
}
