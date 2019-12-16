package dev.ops.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 * A MIDI receiver implementation to handle the button events.
 */
public class MidiMessageReceiver implements Receiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MidiMessageReceiver.class);

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            handle((ShortMessage) message);
        }
    }

    private void handle(ShortMessage sm) {
        LOGGER.info("Received ShortMessage[channel={},command={},data1={},data2={},status={}]",
                sm.getChannel(), sm.getCommand(), sm.getData1(), sm.getData2(), sm.getStatus());
    }

    @Override
    public void close() {
    }
}
