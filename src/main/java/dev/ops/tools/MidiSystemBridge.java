package dev.ops.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

/**
 * Producer implementation to obtain Receiver and Transmitter from MIDI system.
 */
public class MidiSystemBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(MidiSystemBridge.class);

    private Transmitter transmitter;
    private Receiver receiver;

    public void initialize(Launchpad controller) {
        LOGGER.info("Initializing MIDI system.");

        try {
            this.transmitter = MidiSystem.getTransmitter();
            this.transmitter.setReceiver(controller);
        } catch (MidiUnavailableException e) {
            throw new IllegalStateException("Unable to get Transmitter from MIDI system.", e);
        }

        try {
            this.receiver = MidiSystem.getReceiver();
            controller.setReceiver(receiver);
        } catch (MidiUnavailableException e) {
            throw new IllegalStateException("Unable to get Receiver from MIDI system.", e);
        }
    }

    public void destroy() {
        LOGGER.info("Shutdown MIDI system.");

        this.transmitter.close();
        this.receiver.close();
    }
}
