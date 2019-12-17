package dev.ops.tools.midi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 * Abstract Launchpad MIDI device implementation to encapsulate the usage of Javax Sound Midi.
 */
public abstract class LaunchpadDevice implements Receiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchpadDevice.class);

    private Receiver receiver;

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
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
        receiver.close();
    }

    public void square(int row, int col, LaunchpadColor color) {
        int data1 = (row * 16) + col;
        try {
            receiver.send(new ShortMessage(144, 0, data1, color.getValue()), -1);
        } catch (InvalidMidiDataException e) {
            LOGGER.warn("Could not send ShortMessage.", e);
        }
    }

    /**
     * Rest all square buttons to no color.
     */
    public void reset() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                square(i, j, LaunchpadColor.NONE);
            }
        }
    }
}
