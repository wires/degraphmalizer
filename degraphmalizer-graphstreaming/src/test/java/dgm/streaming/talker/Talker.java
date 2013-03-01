package dgm.streaming.talker;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Talker<T> implements Runnable
{
    final private static Logger log = LoggerFactory.getLogger(Talker.class);

    private SomeOne<T> someOne;
    boolean active = false;

    Thread t;

    public Talker(SomeOne<T> someOne)
    {
        this.someOne = someOne;
    }

    public final void start()
    {
        log.info("Starting the thread");
        t = new Thread(this);
        active = true;
        t.start();
    }

    public final void stop() {
        log.info("Stopping the thread");
        active = false;
    }

    @Override
    public final void run()
    {
        while (active)
        {
            T next = nextMessage();
            if (next == null)
            {
                log.info("no more messages");
                someOne.done();
                stop();
                break;
            }
            someOne.talk(next);
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    protected abstract T nextMessage();
}
