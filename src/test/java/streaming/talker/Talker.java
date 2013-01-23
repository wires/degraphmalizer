package streaming.talker;


public abstract class Talker<T> implements Runnable {

    private SomeOne<T> someOne;
    private int i = 0;
    boolean active = false;

    Thread t;

    public Talker(SomeOne<T> someOne) {
        this.someOne = someOne;
    }

    public final void start() {
        System.out.println("Starting the thread");
        t = new Thread(this);
        active = true;
        t.start();
    }

    public final void stop() {
        i = 0;
        System.out.println("Stopping the thread");
        active = false;
    }

    @Override
    public final void run() {
        while (active) {
            T next = nextMessage();
            if (next == null) {
                System.out.println("no more messages");
                someOne.done();
                stop();
                break;
            }
            someOne.talk(next);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }

    protected abstract T nextMessage();
}
