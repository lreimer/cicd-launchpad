package dev.ops.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;

/**
 * Producer implementation to obtain Receiver and Transmitter from MIDI system.
 */
public class MidiSystemBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(MidiSystemBridge.class);

    private Transmitter transmitter;
    private Receiver receiver;

    public void infos() {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            LOGGER.info("Found attached MIDI device {} ({}) - {}", info.getName(), info.getVersion(), info.getDescription());
        }
    }

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
