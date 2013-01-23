package streaming.codec;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;


public class ToHttpChunkEncoder extends OneToOneEncoder {


    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof ChannelBuffer)) {
            return msg;
        }
        ChannelBuffer cb = (ChannelBuffer) msg;
        return new DefaultHttpChunk(ChannelBuffers.copiedBuffer(ChannelBuffers.wrappedBuffer(cb)));
    }
}
