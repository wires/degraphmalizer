package dgm.modules;

import com.google.inject.Inject;
import dgm.Service;
import org.nnsoft.guice.sli4j.core.InjectLogger;
import org.slf4j.Logger;

import java.util.Set;

/**
 * start and stop a set of services
 */
public class ServiceRunner
{
    final Set<Service> services;

    @InjectLogger
    Logger log;

    @Inject
    ServiceRunner(Set<Service> services)
    {
        this.services = services;
    }

    public void startServices()
    {
        for(Service s : services)
        {
            final String serviceName = s.getClass().getSimpleName();
            log.info("Starting {} service", serviceName);
            s.start();
            log.info("Started {} service", serviceName);
        }
    }

    public void stopServices()
    {
        for(Service s : services)
        {
            final String serviceName = s.getClass().getSimpleName();
            log.info("Stopping {} service", serviceName);
            s.stop();
            log.info("Stopped {} service", serviceName);
        }
    }
}
