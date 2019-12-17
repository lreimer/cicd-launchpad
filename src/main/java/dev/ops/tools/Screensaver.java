package dev.ops.tools;

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
}
