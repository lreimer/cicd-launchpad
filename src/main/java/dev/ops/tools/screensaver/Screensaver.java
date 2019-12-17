package dev.ops.tools.screensaver;

import dev.ops.tools.midi.LaunchpadDevice;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Common screensaver interfaces definition.
 */
public interface Screensaver {

    /**
     * Start the screensaver.
     */
    void start();

    /**
     * Stop the screensaver.
     */
    void stop();

    /**
     * Destroy screensaver and any resources.
     */
    void destroy();

    /**
     * Toggle between start and stop.
     */
    void toogle();

    /**
     * Create and return the list of all screensavers for the given launchpad.
     *
     * @param device the launchpad MIDI device instance
     * @return a list of screeensavers
     */
    static List<Screensaver> create(LaunchpadDevice device) {
        return Arrays.asList(
                new RandomScreensaver(device, 250, TimeUnit.MILLISECONDS),
                new WaveScreensaver(device, 175, TimeUnit.MILLISECONDS)
        );
    }
}
