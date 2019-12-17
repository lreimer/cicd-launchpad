package dev.ops.tools;

import dev.ops.tools.cicd.CiCdServer;
import dev.ops.tools.midi.MidiSystemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Main application for the CI/CD Launchpad.
 */
@Command(version = "CI/CD Launchpad 1.0", mixinStandardHelpOptions = true)
class CiCdLaunchpad implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiCdLaunchpad.class);

    @Option(names = {"-t", "--time"}, defaultValue = "5", description = "the refresh time in seconds")
    private long time = 5;

    @Option(names = {"-u", "--user"}, description = "the user name")
    private String user;

    @Option(names = {"-p", "--password"}, arity = "0..1", description = "the passphrase", interactive = true)
    private char[] password;

    @Option(names = "-j", defaultValue = "true", description = "use Jenkins as CI/CD server")
    private boolean jenkins = true;

    @Option(names = {"-f", "--file"}, paramLabel = "JSON_CONFIG", description = "the configuration file")
    private File config;

    public static void main(String[] args) {
        CommandLine.run(new CiCdLaunchpad(), args);
    }

    @Override
    public void run() {
        LOGGER.info("Running CI/CD Launchpad ...");

        CiCdServer ciCdServer = CiCdServer.jenkins(config, time, TimeUnit.SECONDS);
        MidiSystemHandler midiSystem = new MidiSystemHandler();
        midiSystem.infos();

        CiCdLaunchpadController controller = new CiCdLaunchpadController(midiSystem, ciCdServer);
        controller.initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown CI/CD Launchpad.");
            controller.close();
            ciCdServer.destroy();
            midiSystem.destroy();
        }));
    }
}
