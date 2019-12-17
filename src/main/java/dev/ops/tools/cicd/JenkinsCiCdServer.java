package dev.ops.tools.cicd;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * The CiCdServer implementation for Jenkins CI server.
 */
public class JenkinsCiCdServer implements CiCdServer {

    private final File config;
    private final long time;
    private final TimeUnit timeUnit;

    public JenkinsCiCdServer(File config, long time, TimeUnit unit) {
        this.config = config;
        this.time = time;
        this.timeUnit = unit;
    }

    @Override
    public void initialize() {

    }

    @Override
    public void destroy() {

    }
}
