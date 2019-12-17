package dev.ops.tools;

import dev.ops.tools.screensaver.RandomScreensaver;
import dev.ops.tools.screensaver.WaveScreensaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Launchpad controller implementation handles logic for button events and colors.
 */
public class Launchpad implements Receiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(Launchpad.class);

    private Receiver receiver;
    private List<Screensaver> screensavers = new ArrayList<>();

    public void initialize() {
        screensavers.add(new RandomScreensaver(this, 250, TimeUnit.MILLISECONDS));
        screensavers.add(new WaveScreensaver(this, 175, TimeUnit.MILLISECONDS));
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            handle((ShortMessage) message);
        }
    }

    @Override
    public void close() {
        for (Screensaver screensaver : screensavers) {
            screensaver.destroy();
        }
        reset();
        receiver.close();
    }

    private void handle(ShortMessage sm) {
        LOGGER.debug("Received ShortMessage[channel={},command={},data1={},data2={},status={}]",
                sm.getChannel(), sm.getCommand(), sm.getData1(), sm.getData2(), sm.getStatus());

        if (sm.getCommand() == 176 && sm.getData2() == 127) {
            // get index for top button press event
            int index = sm.getData1() - 104;
            if (index >= screensavers.size()) {
                return;
            }

            for (int i = 0; i < screensavers.size(); i++) {
                if (i != index) {
                    screensavers.get(i).stop();
                }
            }

            Screensaver screensaver = screensavers.get(index);
            screensaver.toogle();
        }
    }

    public void reset() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                square(i, j, Color.NONE);
            }
        }
    }

    public void square(int row, int col, Color color) {
        int data1 = (row * 16) + col;
        try {
            receiver.send(new ShortMessage(144, 0, data1, color.getValue()), -1);
        } catch (InvalidMidiDataException e) {
            LOGGER.warn("Could not send ShortMessage.", e);
        }
    }

    void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }
}
