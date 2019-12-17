package dev.ops.tools.screensaver;

import dev.ops.tools.midi.LaunchpadColor;
import dev.ops.tools.midi.LaunchpadDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Screensaver implementation that randomly cycles through the colors.
 */
public class RandomScreensaver implements Screensaver {

    private final LaunchpadDevice launchpad;
    private final long time;
    private final TimeUnit timeUnit;
    private final ScheduledExecutorService executorService;
    private final List<ScheduledFuture<?>> runnables;

    public RandomScreensaver(LaunchpadDevice launchpad, long time, TimeUnit timeUnit) {
        this.launchpad = launchpad;
        this.time = time;
        this.timeUnit = timeUnit;
        this.executorService = Executors.newScheduledThreadPool(2);
        this.runnables = new ArrayList<>();
    }

    @Override
    public void start() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                final int row = i;
                final int col = j;
                runnables.add(executorService.scheduleAtFixedRate(() -> launchpad.square(row, col, LaunchpadColor.random()), 0, time, timeUnit));
            }
        }
    }

    @Override
    public void stop() {
        for (ScheduledFuture<?> f : runnables) {
            f.cancel(true);
        }
        runnables.clear();
    }

    @Override
    public void destroy() {
        this.stop();
        this.executorService.shutdown();
    }

    @Override
    public void toogle() {
        if (runnables.isEmpty()) {
            start();
        } else {
            stop();
            launchpad.reset();
        }
    }
}
