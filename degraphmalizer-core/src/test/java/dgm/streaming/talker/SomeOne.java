package dgm.streaming.talker;

/**
 * Created with IntelliJ IDEA.
 * User: ernst
 * Date: 10/30/12
 * Time: 11:15 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SomeOne<T> {
    public void talk(T message);

    public void done();
}
