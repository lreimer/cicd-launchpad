package dev.ops.tools.cicd;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Interface definition for CI/CD Server implementations.
 */
public interface CiCdServer {
    /**
     * Initialize the CI/CD server.
     */
    void initialize();

    /**
     * Register a callback for CiCdJob updates.
     *
     * @param callback the callback
     */
    void register(Consumer<CiCdJob> callback);

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

    /**
     * Get the CiCdJob for given index.
     *
     * @param index the index of the job
     * @return the CiCdJob or NULL if not found
     */
    CiCdJob getJob(int index);

    /**
     * Trigger a build for the given CiCdJob.
     *
     * @param job the job, may be NULL
     */
    void build(CiCdJob job);
}
