package dev.ops.tools;

import dev.ops.tools.cicd.CiCdJob;
import dev.ops.tools.cicd.CiCdServer;
import dev.ops.tools.midi.LaunchpadColor;
import dev.ops.tools.midi.LaunchpadDevice;
import dev.ops.tools.midi.MidiSystemHandler;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class CiCdLaunchpadControllerTest {

    @Test
    void flashesRunningPadsAndRestoresStaticColorsOnCompletion() {
        RecordingReceiver receiver = new RecordingReceiver();
        StubServer server = new StubServer();
        MidiSystemHandler midi = new MidiSystemHandler() {
            @Override
            public void initialize(LaunchpadDevice device) {
                device.setReceiver(receiver);
            }
        };
        CiCdLaunchpadController controller = new CiCdLaunchpadController(midi, server);
        controller.initialize();
        try {
            assertMessage(receiver.messages.get(0), ShortMessage.CONTROL_CHANGE, 0, 40);
            CiCdJob job = new CiCdJob();
            job.setRow(7);
            job.setResults(List.of("BUILDING", "SUCCESS", "FAILURE", "NOT_BUILT"));
            receiver.messages.clear();
            server.callback.accept(job);

            assertEquals(12, receiver.messages.size());
            for (int column = 0; column < 8; column++) {
                assertMessage(receiver.messages.get(column), ShortMessage.NOTE_ON, 112 + column, 12);
            }
            assertMessage(receiver.messages.get(8), ShortMessage.NOTE_ON, 112, 56);
            assertMessage(receiver.messages.get(9), ShortMessage.NOTE_ON, 113, 60);
            assertMessage(receiver.messages.get(10), ShortMessage.NOTE_ON, 114, 15);
            assertMessage(receiver.messages.get(11), ShortMessage.NOTE_ON, 115, 63);

            for (String result : List.of("SUCCESS", "FAILURE", "ABORTED", "NOT_BUILT")) {
                job.setResults(List.of(result));
                server.callback.accept(job);
                assertMessage(receiver.messages.getLast(), ShortMessage.NOTE_ON, 112,
                        LaunchpadColor.forResult(result).getValue());
            }

            for (LaunchpadColor color : LaunchpadColor.values()) {
                controller.square(0, 0, color);
                assertEquals(12, receiver.messages.getLast().getData2() & 12,
                        "Static colors must copy to both buffers: " + color);
            }
        } finally {
            controller.close();
        }
        assertMessage(receiver.messages.getLast(), ShortMessage.CONTROL_CHANGE, 0, 32);
        assertTrue(receiver.closed);
    }

    private static void assertMessage(ShortMessage message, int command, int note, int value) {
        assertEquals(command, message.getCommand());
        assertEquals(0, message.getChannel());
        assertEquals(note, message.getData1());
        assertEquals(value, message.getData2());
    }

    private static class RecordingReceiver implements Receiver {
        private final List<ShortMessage> messages = new ArrayList<>();
        private boolean closed;

        @Override
        public void send(MidiMessage message, long timeStamp) {
            messages.add((ShortMessage) message);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class StubServer implements CiCdServer {
        private Consumer<CiCdJob> callback;

        @Override
        public void initialize() {}

        @Override
        public void register(Consumer<CiCdJob> callback) {
            this.callback = callback;
        }

        @Override
        public void destroy() {}

        @Override
        public CiCdJob getJob(int index) {
            return null;
        }

        @Override
        public void build(CiCdJob job) {}
    }
}
