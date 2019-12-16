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
    public static final String DEVICE_NAME = "Launchpad Mini MK2";

    @Option(names = {"-t", "--time"}, defaultValue = "5", description = "the refresh time")
    private long time = 5;

    @Option(names = {"-u", "--unit"}, defaultValue = "SECONDS", description = "the time unit")
    private TimeUnit unit = TimeUnit.SECONDS;

    public static void main(String[] args) {
        CommandLine.run(new CiCdLaunchpad(), args);
    }

    @Override
    public void run() {
        // this seems to be required at least under Windows
        System.setProperty("javax.sound.midi.Transmitter", "com.sun.media.sound.MidiInDeviceProvider#" + DEVICE_NAME);
        System.setProperty("javax.sound.midi.Receiver", "com.sun.media.sound.MidiOutDeviceProvider#" + DEVICE_NAME);

        LOGGER.info("Running CI/CD Launchpad ...");

        MidiSystemProducer midiSystem = new MidiSystemProducer();
        midiSystem.initialize();
        midiSystem.register(new MidiMessageReceiver());

        Runtime.getRuntime().addShutdownHook(new Thread(midiSystem::destroy));
    }
}
