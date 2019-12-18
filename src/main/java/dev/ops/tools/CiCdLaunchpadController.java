package dev.ops.tools;

import dev.ops.tools.cicd.CiCdJob;
import dev.ops.tools.cicd.CiCdServer;
import dev.ops.tools.midi.LaunchpadColor;
import dev.ops.tools.midi.LaunchpadDevice;
import dev.ops.tools.midi.MidiSystemHandler;
import dev.ops.tools.screensaver.Screensaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Launchpad controller implementation handles logic for button events and colors.
 */
public class CiCdLaunchpadController extends LaunchpadDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiCdLaunchpadController.class);

    private final MidiSystemHandler midiSystem;
    private final CiCdServer ciCdServer;
    private final List<Screensaver> screensavers;

    public CiCdLaunchpadController(MidiSystemHandler midiSystem, CiCdServer ciCdServer) {
        this.midiSystem = midiSystem;
        this.ciCdServer = ciCdServer;
        this.screensavers = new ArrayList<>();
    }

    public void initialize() {
        midiSystem.initialize(this);
        reset();

        screensavers.addAll(Screensaver.create(this));

        ciCdServer.register(this::update);
        ciCdServer.initialize();
    }

    private void update(CiCdJob job) {
        clear(job.getRow());
        for (int i = 0; i < job.getResults().size(); i++) {
            String result = job.getResult(i);
            square(job.getRow(), i, LaunchpadColor.forResult(result));
        }
    }

    @Override
    protected void handle(int command, int data1, int data2) {
        LOGGER.info("Received MIDI event[command={},data1={},data2={}]", command, data1, data2);

        if (command == 176 && data2 == 127) {
            // a 1-8 button has been pressed
            int index = data1 - 104;
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
        } else if (command == 144 && data2 == 127) {
            // a A-H button has been pressed
            int row = A_H_BUTTONS.indexOf(data1);
            CiCdJob job = ciCdServer.getJob(row);
            ciCdServer.build(job);
        }
    }

    @Override
    public void close() {
        for (Screensaver screensaver : screensavers) {
            screensaver.destroy();
        }
        super.close();
    }
}
