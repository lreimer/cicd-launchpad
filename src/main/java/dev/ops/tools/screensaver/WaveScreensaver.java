package dev.ops.tools.screensaver;

import dev.ops.tools.midi.LaunchpadColor;
import dev.ops.tools.midi.LaunchpadDevice;
import org.apache.commons.collections4.iterators.LoopingIterator;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Screensaver implementation that scrolls like a wave.
 */
public class WaveScreensaver implements Screensaver {

    private final LaunchpadDevice launchpad;
    private final long time;
    private final TimeUnit timeUnit;
    private final ScheduledExecutorService executorService;
    private final List<ScheduledFuture<?>> runnables;
    private final List<LaunchpadColor> waveColors;

    public WaveScreensaver(LaunchpadDevice launchpad, long time, TimeUnit timeUnit) {
        this.launchpad = launchpad;
        this.time = time;
        this.timeUnit = timeUnit;
        this.executorService = Executors.newScheduledThreadPool(2);
        this.runnables = new ArrayList<>();
        this.waveColors = new ArrayList<>();

        waveColors.add(LaunchpadColor.BRIGHT_RED);
        waveColors.add(LaunchpadColor.MEDIUM_RED);
        waveColors.add(LaunchpadColor.DARK_RED);
        waveColors.add(LaunchpadColor.BRIGHT_AMBER);
        waveColors.add(LaunchpadColor.MEDIUM_AMBER);
        waveColors.add(LaunchpadColor.DARK_AMBER);
        waveColors.add(LaunchpadColor.BRIGHT_YELLOW);
        waveColors.add(LaunchpadColor.MEDIUM_YELLOW);
        waveColors.add(LaunchpadColor.DARK_YELLOW);
        waveColors.add(LaunchpadColor.BRIGHT_GREEN);
        waveColors.add(LaunchpadColor.MEDIUM_GREEN);
        waveColors.add(LaunchpadColor.DARK_GREEN);
        waveColors.add(LaunchpadColor.NONE);
    }

    @Override
    public void start() {
        launchpad.reset();
        for (int i = 0; i < 8; i++) {
            final int row = i;
            runnables.add(executorService.scheduleAtFixedRate(new WaveRunner(row), 0, time, timeUnit));
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

    private class WaveRunner implements Runnable {

        private final int row;
        private final LoopingIterator<LaunchpadColor> iterator;
        private final CircularFifoQueue<LaunchpadColor> queue;

        public WaveRunner(int row) {
            this.row = row;
            this.iterator = new LoopingIterator<>(waveColors);
            this.queue = new CircularFifoQueue<>(8);
        }

        @Override
        public void run() {
            int index = 7;
            for (LaunchpadColor color : queue) {
                launchpad.square(row, index, color);
                index--;
            }

            // add the next color to the queue
            queue.add(iterator.next());
        }
    }
}
