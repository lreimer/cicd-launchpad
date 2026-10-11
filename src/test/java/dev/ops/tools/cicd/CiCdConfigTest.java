package dev.ops.tools.cicd;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CiCdConfigTest {

    @TempDir
    Path directory;

    @Test
    void readsExistingConfigurationWithJakartaJsonProvider() throws Exception {
        CiCdConfig config = CiCdConfig.fromFile(
                Path.of(getClass().getResource("/jenkins-jobs.json").toURI()).toFile());

        assertEquals("http://localhost:8080/", config.getUrl());
        assertEquals("admin", config.getUsername());
        assertEquals("admin", config.getPassword());
        assertEquals(2, config.getJobs().size());
        assertEquals("Test 1", config.getJobs().get(0).getName());
        assertEquals(0, config.getJobs().get(0).getRow());
        assertEquals("Test/Test 2", config.getJobs().get(1).getName());
        assertEquals(1, config.getJobs().get(1).getRow());
    }

    @Test
    void mapsGitHubActionsBuildAndDemosToAllEightRows() throws Exception {
        CiCdConfig config = CiCdConfig.fromFile(
                Path.of(getClass().getResource("/github-actions-jobs.json").toURI()).toFile());
        String[] workflows = {
                "gradle.yml", "demo-success.yml", "demo-failure.yml", "demo-skipped.yml",
                "demo-cancelled.yml", "demo-timeout.yml", "demo-alternating.yml", "demo-running.yml"
        };

        assertEquals("lreimer/cicd-launchpad", config.getRepository());
        assertEquals("master", config.getBranch());
        assertEquals(workflows.length, config.getJobs().size());
        for (int row = 0; row < workflows.length; row++) {
            assertEquals(row, config.getJobs().get(row).getRow());
            assertEquals(workflows[row], config.getJobs().get(row).getName());
        }
    }

    @Test
    void preservesConfigurationLimits() {
        CiCdConfig config = new CiCdConfig();
        assertThrows(IllegalArgumentException.class,
                () -> config.setJobs(Collections.nCopies(9, new CiCdJob())));
        assertThrows(IllegalArgumentException.class, () -> new CiCdJob().setRow(-1));
        assertThrows(IllegalArgumentException.class, () -> new CiCdJob().setRow(8));

        CiCdJob job = CiCdJob.fromJson(Json.createObjectBuilder()
                .add("jobs", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("name", "last").add("row", 7)))
                .build()).get(0);
        assertEquals(7, job.getRow());
    }

    @Test
    void reportsUnreadableAndMalformedConfiguration() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> CiCdConfig.fromFile(directory.resolve("missing.json").toFile()));
        Path malformed = directory.resolve("malformed.json");
        Files.writeString(malformed, "{ invalid }");
        assertThrows(jakarta.json.stream.JsonParsingException.class,
                () -> CiCdConfig.fromFile(malformed.toFile()));
    }
}
