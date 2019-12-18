package dev.ops.tools.cicd;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The CiCdServer implementation for Jenkins CI server.
 */
public class JenkinsCiCdServer implements CiCdServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsCiCdServer.class);

    private final File configFile;
    private final long time;
    private final TimeUnit unit;
    private final Set<Consumer<CiCdJob>> callbacks;

    private CiCdConfig config;
    private ScheduledExecutorService executorService;
    private JenkinsServer jenkins;


    public JenkinsCiCdServer(File configFile, long time, TimeUnit unit) {
        this.configFile = configFile;
        this.time = time;
        this.unit = unit;
        this.callbacks = new HashSet<>();
    }

    @Override
    public void initialize() {
        config = CiCdConfig.fromFile(configFile);
        LOGGER.info("Using {}", config);

        jenkins = new JenkinsServer(config.getURI(), config.getUsername(), config.getPassword());
        LOGGER.info("Connected to Jenkins server {}", config.getUrl());

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(this::update, time, time, unit);
    }

    @Override
    public void register(Consumer<CiCdJob> callback) {
        callbacks.add(callback);
    }

    private void notify(CiCdJob job) {
        for (Consumer<CiCdJob> c : callbacks) {
            c.accept(job);
        }
    }

    private void update() {
        LOGGER.info("Updating CI/CD jobs ...");
        for (CiCdJob job : config.getJobs()) {
            try {
                JobWithDetails jobWithDetails = jenkins.getJob(job.getName());
                if (jobWithDetails == null) {
                    LOGGER.warn("Unable to get details for Job {}", job.getName());
                    continue;
                }

                List<Build> builds = jobWithDetails.getBuilds();
                int fromIndex = builds.size() < 9 ? 0 : builds.size() - 8;

                updateCiCdJobResults(job, builds.subList(fromIndex, builds.size()));
                notify(job);
            } catch (IOException e) {
                LOGGER.warn("Unable to get Job with details.", e);
            }
        }
    }

    private void updateCiCdJobResults(CiCdJob job, List<Build> builds) throws IOException {
        LOGGER.info("   - {}", job.getName());

        job.getResults().clear();

        for (Build build : builds) {
            BuildWithDetails details = build.details();
            job.addResult(details.getResult().name());
        }
    }

    @Override
    public void destroy() {
        executorService.shutdown();
        jenkins.close();
    }
}
