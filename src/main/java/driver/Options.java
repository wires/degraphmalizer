package driver;

import com.beust.jcommander.Parameter;

import java.util.*;

public class Options
{
    @Parameter(names = {"-p", "--port"}, description = "Listening port")
    int port;

    @Parameter(names = {"-l", "--local" }, description = "Run ES locally")
    boolean local;

    @Parameter(names = {"-t", "--transport" }, description = "Run against remote ES (cluster, host, port)", arity = 3)
    List<String> transport = new ArrayList<String>();

    @Parameter(names = {"-s", "--scriptDir"}, description = "Specify script directory")
    String script;

    @Parameter(names = {"-j", "--jmx"}, description = "Enable JMX monitoring bean")
    boolean jmx;

    @Parameter(names = {"-r", "--reload"}, description = "Enable automatic configuration reloading")
    boolean reloading;

    @Parameter(names = {"-L", "--logback"}, description = "Specify logback configuration file")
    String logbackConf = "logback.xml";

    @Parameter(names = {"--help"}, help = true)
    boolean help;

    public Options()
    {
        this(System.getProperties());
    }

    /**
     * You can pass the system properties here, this will then be used as defaults that can be overridden using the CLI.
     *
     * @param properties
     */
    public Options(Properties properties)
    {
        port = Integer.parseInt(properties.getProperty("server.port", "9821"));
        jmx = Boolean.parseBoolean(properties.getProperty("degraphmalizer.jmx.enabled"));
        reloading = Boolean.parseBoolean(properties.getProperty("degraphmalizer.autoreload"));

        // try to set the defaults for local
        local = properties.getProperty("elasticsearch.local", "false").equals("true");

        if(!local)
        {
            // try to set the defaults for a cluster
            final String cluster = properties.getProperty("elasticsearch.cluster");
            final String host = properties.getProperty("elasticsearch.host");
            final String port = properties.getProperty("elasticsearch.port");

            if(cluster != null && host != null)
            {
                transport.add(cluster);
                transport.add(host);
                transport.add(port != null ? port : "9300");
            }
        }

        script = properties.getProperty("scripts.dir", "scripts");
    }
}
