package dgm.fixtures;

/**
 * @author Ernst Bunders
 */
public class DummyCommand implements Command<Boolean>
{
    @Override
    public Boolean execute() { return true; }
}
