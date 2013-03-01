package dgm.driver;

import com.beust.jcommander.Parameter;

import java.util.*;

public class Options
{
    @Parameter(names = {"-t", "--transport" }, description = "Run against remote ES (host, port, cluster)",
            arity = 3)
    List<String> transport = new ArrayList<String>();

    @Parameter(names = {"-d", "--development"}, description = "Run in development mode")
    boolean development = false;

    @Parameter(names = {"-p", "--port"}, description = "Listening port")
    int port;

    @Parameter(names = {"-c", "--config"}, description = "Specify configuration directory")
    String config;

    @Parameter(names = {"-g", "--graphdb"}, description = "Specify graph DB storage directory")
    String graphdb;

    @Parameter(names = {"-j", "--jmx"}, description = "Enable JMX monitoring bean")
    boolean jmx;

    @Parameter(names = {"-r", "--reload"}, description = "Enable automatic configuration reloading")
    boolean reloading;

    @Parameter(names = {"-L", "--logback"}, description = "Specify logback configuration file")
    String logbackConf = "logback.xml";

    @Parameter(names = {"-f", "--fixtures"}, description = "Load fixtures on startup")
    boolean fixtures;

    @Parameter(names = {"--jslib"}, description = "Load Javascript library from this file")
    List<String> libraries = new ArrayList<String>();

    @Parameter(names = {"-?", "--help"}, help = true)
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
        port = Integer.parseInt(properties.getProperty("degraphmalizer.port", "9821"));
        jmx = Boolean.parseBoolean(properties.getProperty("degraphmalizer.jmx.enabled"));
        reloading = Boolean.parseBoolean(properties.getProperty("degraphmalizer.autoreload"));
        fixtures = Boolean.parseBoolean(properties.getProperty("degraphmalizer.fixtures"));

        // try to set the defaults for a cluster
        transport.add(properties.getProperty("elasticsearch.host", "localhost"));
        transport.add(properties.getProperty("elasticsearch.port", "9300"));
        transport.add(properties.getProperty("elasticsearch.cluster", "elasticsearch"));

        config = properties.getProperty("paths.config", "conf");
        graphdb = properties.getProperty("paths.graphdb", "data/graphdb");
    }
}
