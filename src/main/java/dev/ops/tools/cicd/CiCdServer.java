package dev.ops.tools.cicd;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Interface definition for CI/CD Server implementations.
 */
public interface CiCdServer {
    /**
     * Initialize the CI/CD server.
     */
    void initialize();

    /**
     * Destroy the instance.
     */
    void destroy();

    /**
     * Factory method for the Jenkins CI/CD server.
     *
     * @param config   the JSON config file
     * @param time     the time to check for changes
     * @param timeUnit the time unit
     * @return a Jenkins CI/CD server instance
     */
    static CiCdServer jenkins(File config, long time, TimeUnit timeUnit) {
        return new JenkinsCiCdServer(config, time, timeUnit);
    }
}
