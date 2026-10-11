package dev.ops.tools;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.StringWriter;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

class CiCdLaunchpadTest {

    @Test
    void helpAndVersionDoNotRequireConfigurationOrMidi() {
        CommandLine command = new CommandLine(new CiCdLaunchpad());
        StringWriter output = new StringWriter();
        command.setOut(new PrintWriter(output));

        assertEquals(0, command.execute("--help"));
        assertTrue(output.toString().contains("JSON_CONFIG"));
        output.getBuffer().setLength(0);
        assertEquals(0, command.execute("--version"));
        assertTrue(output.toString().contains("CI/CD Launchpad 1.0"));
    }

    @Test
    void preservesCommandLineOptions() {
        CommandLine command = new CommandLine(new CiCdLaunchpad());
        CommandLine.ParseResult result = command.parseArgs(
                "-f", "jobs.json", "-t", "5", "-u", "user", "-p", "token");

        assertEquals(5L, result.<Long>matchedOptionValue("-t", null));
        assertEquals("user", result.matchedOptionValue("-u", null));
        assertEquals("token", result.matchedOptionValue("-p", null));
        assertEquals("jobs.json", result.matchedOptionValue("-f", null).toString());

        result = new CommandLine(new CiCdLaunchpad()).parseArgs("-g", "-f", "github.json");
        assertTrue(result.<Boolean>matchedOptionValue("--github", false));
    }

    @Test
    void rejectsMissingConfiguration() {
        CommandLine command = new CommandLine(new CiCdLaunchpad());
        command.setErr(new PrintWriter(new StringWriter()));

        assertEquals(CommandLine.ExitCode.USAGE, command.execute());
    }
}
