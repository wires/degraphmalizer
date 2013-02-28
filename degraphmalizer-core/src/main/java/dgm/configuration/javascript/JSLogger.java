package dgm.configuration.javascript;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JSLogger {
    Logger log = LoggerFactory.getLogger(JSLogger.class);

    public void log(String msg) {
        log.info(msg);
    }
}
