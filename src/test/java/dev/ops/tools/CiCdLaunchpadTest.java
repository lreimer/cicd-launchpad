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
        assertTrue(output.toString().contains("CI/CD Launchpad 2.0"));
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

        result = new CommandLine(new CiCdLaunchpad()).parseArgs("-j", "-f", "jenkins.json");
        assertTrue(result.<Boolean>matchedOptionValue("--jenkins", false));
    }

    @Test
    void selectsGitHubActionsByDefaultAndJenkinsOnRequest() {
        CiCdLaunchpad launchpad = new CiCdLaunchpad();
        new CommandLine(launchpad).parseArgs("-f", "jobs.json");
        assertFalse(launchpad.useJenkins());

        launchpad = new CiCdLaunchpad();
        new CommandLine(launchpad).parseArgs("-j", "-f", "jobs.json");
        assertTrue(launchpad.useJenkins());
    }

    @Test
    void rejectsGitHubAndJenkinsTogether() {
        CommandLine command = new CommandLine(new CiCdLaunchpad());
        command.setErr(new PrintWriter(new StringWriter()));

        assertEquals(CommandLine.ExitCode.USAGE, command.execute("-g", "-j", "-f", "jobs.json"));
    }

    @Test
    void rejectsMissingConfiguration() {
        CommandLine command = new CommandLine(new CiCdLaunchpad());
        command.setErr(new PrintWriter(new StringWriter()));

        assertEquals(CommandLine.ExitCode.USAGE, command.execute());
    }
}
