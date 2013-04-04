package dgm.driver;

import com.beust.jcommander.JCommander;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import dgm.driver.handler.HandlerModule;
import dgm.driver.server.Server;
import dgm.driver.server.ServerModule;
import dgm.fixutures.FixtureLoaderModule;
import dgm.fixutures.FixturesLoader;
import dgm.jmx.GraphBuilder;
import dgm.modules.BlueprintsSubgraphManagerModule;
import dgm.modules.DegraphmalizerModule;
import dgm.modules.ServiceRunner;
import dgm.modules.ThreadpoolModule;
import dgm.modules.elasticsearch.CommonElasticSearchModule;
import dgm.modules.elasticsearch.nodes.LocalES;
import dgm.modules.elasticsearch.nodes.NodeES;
import dgm.modules.fsmon.DynamicConfiguration;
import dgm.modules.fsmon.StaticConfiguration;
import dgm.modules.neo4j.CommonNeo4j;
import dgm.modules.neo4j.EmbeddedNeo4J;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.nnsoft.guice.sli4j.slf4j.Slf4jLoggingModule;
import org.slf4j.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class Main
{
    private static final String LOGBACK_CFG = "logback.configurationFile";

    @InjectLogger
    Logger log;

    private Main(String[] args)
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

        // find logback settings file
        if (System.getProperty(LOGBACK_CFG) == null)
            System.setProperty(LOGBACK_CFG, opt.logbackConf);

        // check if script directory exists
        if(!new File(opt.config).isDirectory())
            exit("Cannot find configuration directory " + opt.config + " Exiting.");

        System.out.println("Automatic configuration reloading: " + (opt.reloading ? "enabled" : "disabled"));

        // depending on properties / CLI, load proper modules
        final List<Module> modules = new ArrayList<Module>();

        // some defaults
        modules.add(new BlueprintsSubgraphManagerModule());
        modules.add(new Slf4jLoggingModule());
        modules.add(new DegraphmalizerModule());
        modules.add(new ThreadpoolModule());

        // netty part
        modules.add(new ServerModule(opt.port));
        modules.add(new HandlerModule());

        // we always run an embedded local graph database
        modules.add(new CommonNeo4j());
        modules.add(new EmbeddedNeo4J(opt.graphdb));

        // elasticsearch setup
        setupElasticsearch(opt, modules);

        // configuration reloading etc
        setupConfiguration(opt, modules);

        // the injector
        final Injector injector = Guice.createInjector(modules);

        // logger
        injector.injectMembers(this);

        // start JMX?
        if(opt.jmx)
        {
            // setup our JMX bean
            try
            {
                log.info("Starting JMX");
                final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                final ObjectName name = new ObjectName("graph.mbeans:type=RandomizedGraphBuilder");
                final GraphBuilder gb = injector.getInstance(GraphBuilder.class);
                mbs.registerMBean(gb, name);
                log.info("JMX bean {} started", name);
            }
            catch (Exception e)
            {
                // TODO log errors
                e.printStackTrace();
            }
        }

        // start fixtures
        if (opt.fixtures)
        {
            FixturesLoader fl = injector.getInstance(FixturesLoader.class);
            try
            {
                fl.loadFixtures();
            } catch (Exception e)
            {
                log.error("Could not load fixtures. reason: {}", e.getMessage());
            }
        }

        final Server server = injector.getInstance(Server.class);
        final ServiceRunner runner = injector.getInstance(ServiceRunner.class);

        // so we can shutdown cleanly
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                log.info("JVM Shutdown received (e.g., Ctrl-c pressed)");
                server.stopAndWait();

                runner.stopServices();
            }
        });

        // start services and then the main netty server
        runner.startServices();

        log.info("Starting server on port {}", opt.port);
        server.startAndWait();
    }

    private void setupElasticsearch(Options opt, List<Module> modules)
    {
        modules.add(new CommonElasticSearchModule());

        // setup local node
        if(opt.development)
        {
            modules.add(new LocalES());
            return;
        }

        // setup node that connects to remote host
        if(opt.transport.size() != 3)
            exit("You need to specify either the local or transport ES config. Exiting.");

        final String cluster = opt.transport.get(2);
        final String host = opt.transport.get(0);
        final int port = Integer.parseInt(opt.transport.get(1));

        modules.add(new NodeES(cluster, host, port));
    }

    private void setupConfiguration(Options opt, List<Module> modules)
    {
        // automatic reloading
        if(opt.reloading)
            modules.add(new DynamicConfiguration(opt.config, opt.libraries()));
        else
            modules.add(new StaticConfiguration(opt.config, opt.libraries()));

        // fixtures
        if(opt.fixtures)
            modules.add(new FixtureLoaderModule(createRunMode(opt)));
    }

    public static void main(String[] args)
    {
        new Main(args);
    }

    private static void exit(String message)
    {
        System.err.println(message);
        System.exit(1);
    }

    private static RunMode createRunMode(Options options)
    {
        if(options.reloading && options.fixtures){
            return RunMode.DEVELOPMENT;
        }else if(options.reloading){
            return RunMode.TEST;
        }
        return RunMode.PRODUCTION;
    }
}