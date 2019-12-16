package dev.ops.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Screensaver {

    private final Launchpad launchpad;
    private final long time;
    private final TimeUnit timeUnit;

    private ScheduledExecutorService executorService;
    private List<ScheduledFuture<?>> runnables;

    public Screensaver(Launchpad launchpad, long time, TimeUnit timeUnit) {
        this.launchpad = launchpad;
        this.time = time;
        this.timeUnit = timeUnit;
    }

    public void initialize() {
        executorService = Executors.newScheduledThreadPool(2);
        runnables = new ArrayList<>();
    }

    public void start() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                final int row = i;
                final int col = j;

                runnables.add(executorService.scheduleAtFixedRate(() -> launchpad.square(row, col, Color.random()), 0, time, timeUnit));
            }
        }
    }

    public void stop() {
        for (ScheduledFuture<?> f : runnables) {
            f.cancel(true);
        }
        runnables.clear();
    }

    public void destroy() {
        this.stop();
        this.executorService.shutdown();
    }
}
