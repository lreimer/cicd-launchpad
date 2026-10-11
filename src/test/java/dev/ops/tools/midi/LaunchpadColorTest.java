package dev.ops.tools.midi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LaunchpadColorTest {

    @Test
    void preservesSuccessAndFailureColorsIgnoringCase() {
        assertEquals(LaunchpadColor.BRIGHT_GREEN, LaunchpadColor.forResult("SUCCESS"));
        assertEquals(LaunchpadColor.BRIGHT_GREEN, LaunchpadColor.forResult("success"));
        assertEquals(LaunchpadColor.BRIGHT_RED, LaunchpadColor.forResult("FAILURE"));
        assertEquals(LaunchpadColor.BRIGHT_RED, LaunchpadColor.forResult("failure"));
    }

    @Test
    void displaysBuildingResultsInGreen() {
        assertEquals(LaunchpadColor.BRIGHT_GREEN, LaunchpadColor.forResult("BUILDING"));
        assertEquals(LaunchpadColor.BRIGHT_GREEN, LaunchpadColor.forResult("building"));
    }

    @Test
    void displaysAllOtherResultsInYellow() {
        for (String result : new String[] {
                "ABORTED", "NOT_BUILT", "UNSTABLE", "unknown", "", null
        }) {
            assertEquals(LaunchpadColor.BRIGHT_YELLOW, LaunchpadColor.forResult(result),
                    () -> "Result: " + result);
        }
    }

    @Test
    void preservesUnlitColorForClearingPads() {
        assertEquals(64, LaunchpadColor.NONE.getValue());
    }
}
