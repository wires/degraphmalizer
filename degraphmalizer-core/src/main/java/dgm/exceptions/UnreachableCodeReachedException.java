package dgm.exceptions;

/**
 *
 */
public class UnreachableCodeReachedException extends DegraphmalizerException
{
    // TODO refactor all code that has to resort to this exception
    public UnreachableCodeReachedException()
    {
        super("Sweet holy Jezus, the unimaginable has happened! A block of unreachable code has been reached!");
    }
}
