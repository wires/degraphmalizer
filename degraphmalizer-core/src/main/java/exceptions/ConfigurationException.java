package exceptions;

/**
 * Something is wrong while loading the {@link configuration.Configuration}.
 */
public class ConfigurationException extends DegraphmalizerException
{
    public ConfigurationException(String msg)
    {
        super(msg);
    }
}
