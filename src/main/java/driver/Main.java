package driver;

import com.beust.jcommander.JCommander;
import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.tinkerpop.blueprints.Graph;
import degraphmalizr.DegraphmalizerModule;
import driver.handler.HandlerModule;
import driver.server.Server;
import driver.server.ServerModule;
import elasticsearch.LocalES;
import elasticsearch.TransportES;
import jmx.GraphBuilder;
import modules.*;
import neo4j.*;
import org.elasticsearch.client.Client;
import org.nnsoft.guice.sli4j.slf4j.Slf4jLoggingModule;
import org.slf4j.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class Main
{
    public static void main(String[] args)
    {
        final Options opt = new Options();

        // parse CLI options
        final JCommander jcommander = new JCommander(opt, args);

        // print help and exit
        if(opt.help)
        {
            jcommander.usage();
            System.exit(1);
        }

        // check if script directory exists
        if(!new File(opt.config).isDirectory())
        {
            System.err.println("Cannot find script directory '" + opt.config + "'");
            System.exit(1);
        }

        // find logback settings file
        final String LOGBACK_CFG = "logback.configurationFile";
        if (System.getProperty(LOGBACK_CFG) == null)
            System.setProperty(LOGBACK_CFG, "file:" + opt.logbackConf);

        // depending on properties / CLI, load proper modules
        final List<Module> modules = new ArrayList<Module>();
        modules.add(new ServerModule(opt.port));
        modules.add(new HandlerModule());

        // setup elasticsearch transport node
        if(!(opt.transport.size() == 3))
        {
            System.err.println("You need to specify either the local or transport ES config");
            System.exit(0);
        }

        final String cluster = opt.transport.get(0);
        final int port = Integer.parseInt(opt.transport.get(1));
        final String host = opt.transport.get(2);
        modules.add(new TransportES(cluster, host, port));

        // we always run an embedded local graph database
        modules.add(new EmbeddedNeo4J(opt.graphdb));

        // some defaults
        modules.add(new CommonNeo4j());
        modules.add(new BlueprintsSubgraphManagerModule());
        modules.add(new Slf4jLoggingModule(Matchers.any()));
        modules.add(new LogconfModule());
        modules.add(new DegraphmalizerModule());
        modules.add(new ThreadpoolModule());

        // automatic reloading
        if(opt.reloading)
        {
            // TODO logging
            System.err.println("Automatic reloading: enabled");
            try
            {
                modules.add(new ReloadingJSConfModule(opt.config));
            }
            catch (IOException e)
            {
                System.err.println("Failed parsing the configuration");
                System.exit(1);
            }
        }
        else
        {
            // TODO logging
            System.err.println("Automatic reloading: disabled");
            modules.add(new StaticJSConfModule(opt.config));
        }

        // the injector
        final Injector injector = Guice.createInjector(modules);

        // logger
        final Logger logger = injector.getInstance(Logger.class);

        // start JMX?
        if(opt.jmx)
        {
            // setup our JMX bean
            try
            {
                // TODO log JMX
                final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                final ObjectName name = new ObjectName("graph.mbeans:type=GraphBuilder");
                final GraphBuilder gb = injector.getInstance(GraphBuilder.class);
                mbs.registerMBean(gb, name);
            }
            catch (Exception e)
            {
                // TODO log errors
                e.printStackTrace();
            }
        }

        // the netty machine
        final Server server = injector.getInstance(Server.class);

        // so we can shutdown cleanly
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {

                logger.info("JVM Shutdown received (e.g., Ctrl-c pressed)");
                server.stopAndWait();

                // shutdown ES
                logger.info("Terminating ElasticSearch client connection");
                final Client client = injector.getInstance(Client.class);
                client.close();

                // shutdown the graph
                logger.info("Terminating graph database connection");
                final Graph graph = injector.getInstance(Graph.class);
                graph.shutdown();
            }
        });

        // start listening
        logger.info("Starting server at {}", opt.port);
        server.startAndWait();
    }
}