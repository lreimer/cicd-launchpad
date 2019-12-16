package dev.ops.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class Launchpad implements Receiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(Launchpad.class);

    private Receiver receiver;

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            handle((ShortMessage) message);
        }
    }

    @Override
    public void close() {
    }

    private void handle(ShortMessage sm) {
        LOGGER.info("Received ShortMessage[channel={},command={},data1={},data2={},status={}]",
                sm.getChannel(), sm.getCommand(), sm.getData1(), sm.getData2(), sm.getStatus());
    }

    void reset() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                square(i, j, Color.NONE);
            }
        }
    }

    void square(int row, int col, Color color) {
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
