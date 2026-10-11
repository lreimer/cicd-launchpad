package dev.ops.tools.screensaver;

import dev.ops.tools.midi.LaunchpadColor;
import dev.ops.tools.midi.LaunchpadDevice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WaveScreensaverTest {

    private record Square(int column, LaunchpadColor color) {}

    @Test
    void scrollsColorsAndEvictsOldestAfterEightSquares() throws InterruptedException {
        List<Square> squares = new CopyOnWriteArrayList<>();
        CountDownLatch rendered = new CountDownLatch(44);
        LaunchpadDevice device = new LaunchpadDevice() {
            @Override
            protected void handle(int command, int data1, int data2) {}

            @Override
            public void reset() {}

            @Override
            public void square(int row, int column, LaunchpadColor color) {
                if (row == 0) {
                    squares.add(new Square(column, color));
                    rendered.countDown();
                }
            }
        };
        WaveScreensaver screensaver = new WaveScreensaver(device, 10, TimeUnit.MILLISECONDS);
        try {
            screensaver.start();
            assertTrue(rendered.await(5, TimeUnit.SECONDS), "Wave did not render nine frames");
            screensaver.stop();

            assertEquals(new Square(7, LaunchpadColor.BRIGHT_RED), squares.get(0));
            assertEquals(new Square(7, LaunchpadColor.BRIGHT_RED), squares.get(1));
            assertEquals(new Square(6, LaunchpadColor.MEDIUM_RED), squares.get(2));
            assertEquals(List.of(
                    new Square(7, LaunchpadColor.MEDIUM_RED),
                    new Square(6, LaunchpadColor.DARK_RED),
                    new Square(5, LaunchpadColor.BRIGHT_AMBER),
                    new Square(4, LaunchpadColor.MEDIUM_AMBER),
                    new Square(3, LaunchpadColor.DARK_AMBER),
                    new Square(2, LaunchpadColor.BRIGHT_YELLOW),
                    new Square(1, LaunchpadColor.MEDIUM_YELLOW),
                    new Square(0, LaunchpadColor.DARK_YELLOW)), squares.subList(36, 44));
        } finally {
            screensaver.destroy();
        }
    }
}
