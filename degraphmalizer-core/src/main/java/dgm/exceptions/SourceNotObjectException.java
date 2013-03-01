package dgm.exceptions;

// TODO in newer model reduce has type :: Doc -> GraphCtx -> Json, degraphmalizer doesn't know about multiple props anymore, so then this can be removed
public class SourceNotObjectException extends DegraphmalizerException
{
    public SourceNotObjectException()
    {
        super("Source document is not a JSON object (ie. it's a value), cannot add attributes");
    }
}
