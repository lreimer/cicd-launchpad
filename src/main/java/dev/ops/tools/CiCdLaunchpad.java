package dev.ops.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.TimeUnit;

/**
 * Main application for the CI/CD Launchpad.
 */
@Command(version = "CI/CD Launchpad 1.0", mixinStandardHelpOptions = true)
class CiCdLaunchpad implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiCdLaunchpad.class);
    private static final String DEVICE_NAME = "Launchpad Mini";

    @Option(names = {"-t", "--time"}, defaultValue = "500", description = "the refresh time")
    private long time = 500;

    @Option(names = {"-u", "--unit"}, defaultValue = "MILLISECONDS", description = "the time unit")
    private TimeUnit unit = TimeUnit.MILLISECONDS;

    public static void main(String[] args) {
        CommandLine.run(new CiCdLaunchpad(), args);
    }

    @Override
    public void run() {
        // this seems to be required at least under Windows
        System.setProperty("javax.sound.midi.Transmitter", "com.sun.media.sound.MidiInDeviceProvider#" + DEVICE_NAME);
        System.setProperty("javax.sound.midi.Receiver", "com.sun.media.sound.MidiOutDeviceProvider#" + DEVICE_NAME);

        LOGGER.info("Running CI/CD Launchpad ...");

        MidiSystemBridge midiSystem = new MidiSystemBridge();
        Launchpad launchpad = new Launchpad();

        midiSystem.infos();
        midiSystem.initialize(launchpad);

        launchpad.initialize();
        launchpad.reset();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown CI/CD Launchpad.");
            launchpad.close();
            midiSystem.destroy();
        }));
    }
}
