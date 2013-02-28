package dgm.streaming.talker;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.HttpChunk;

/**
 * Created with IntelliJ IDEA.
 * User: ernst
 * Date: 11/5/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringSomeOne implements SomeOne<String> {
    private Channel channel;

    StringSomeOne(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void talk(String words) {
        //HttpChunk chunk = new DefaultHttpChunk(ChannelBuffers.copiedBuffer(words.getBytes()));
        channel.write(words);
    }

    @Override
    public void done() {
        ChannelFuture future = channel.write(HttpChunk.LAST_CHUNK);
        try {
            future.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.close();
    }
}
