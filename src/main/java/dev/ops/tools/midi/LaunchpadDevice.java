package dev.ops.tools.midi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract Launchpad MIDI device implementation to encapsulate the usage of Javax Sound Midi.
 */
public abstract class LaunchpadDevice implements Receiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchpadDevice.class);

    private Receiver receiver;

    protected static final List<Integer> A_H_BUTTONS = Arrays.asList(8, 24, 40, 56, 72, 88, 104, 120);

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
        sendMessage(ShortMessage.CONTROL_CHANGE, 0, 40);
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            ShortMessage shortMessage = (ShortMessage) message;
            handle(shortMessage.getCommand(), shortMessage.getData1(), shortMessage.getData2());
        }
    }

    protected abstract void handle(int command, int data1, int data2);

    @Override
    public void close() {
        reset();
        sendMessage(ShortMessage.CONTROL_CHANGE, 0, 32);
        receiver.close();
    }

    public void square(int row, int col, LaunchpadColor color) {
        square(row, col, color, false);
    }

    public void square(int row, int col, LaunchpadColor color, boolean flashing) {
        int data1 = (row * 16) + col;
        // Static colors copy to both buffers; flashing colors clear the other buffer.
        int velocity = (color.getValue() & 0x33) | (flashing ? 8 : 12);
        sendMessage(ShortMessage.NOTE_ON, data1, velocity);
    }

    private void sendMessage(int command, int data1, int data2) {
        try {
            receiver.send(new ShortMessage(command, 0, data1, data2), -1);
        } catch (InvalidMidiDataException e) {
            LOGGER.warn("Could not send ShortMessage.", e);
        }
    }

    public void clear(int row) {
        for (int j = 0; j < 8; j++) {
            square(row, j, LaunchpadColor.NONE);
        }
    }

    /**
     * Rest all square buttons to no color.
     */
    public void reset() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 9; j++) {
                square(i, j, LaunchpadColor.NONE);
            }
        }
    }
}
