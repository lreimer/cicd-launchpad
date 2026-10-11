package dev.ops.tools.cicd;

import com.sun.net.httpserver.HttpServer;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GitHubActionsCiCdServerTest {

    @TempDir
    Path directory;

    @Test
    void pollsWorkflowRunsAndDispatchesWorkflow() throws Exception {
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String url = "http://127.0.0.1:" + http.getAddress().getPort() + "/";
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> dispatchBody = new AtomicReference<>();
        List<String> requests = new CopyOnWriteArrayList<>();
        http.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            requests.add(exchange.getRequestMethod() + " " + path + (query == null ? "" : "?" + query));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String response = "";
            int status = 200;
            if (path.equals("/repos/owner/repo/actions/workflows/build.yml/runs")) {
                response = """
                        {"total_count":5,"workflow_runs":[
                          {"id":5,"status":"in_progress","conclusion":null},
                          {"id":4,"status":"completed","conclusion":"success"},
                          {"id":3,"status":"completed","conclusion":"failure"},
                          {"id":2,"status":"completed","conclusion":"cancelled"},
                          {"id":1,"status":"completed","conclusion":"timed_out"}]}
                        """;
            } else if (path.equals("/repos/owner/repo")) {
                response = "{\"default_branch\":\"main\"}";
            } else if (path.equals("/repos/owner/repo/actions/workflows/build.yml/dispatches")
                    && exchange.getRequestMethod().equals("POST")) {
                dispatchBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                status = 204;
            } else {
                response = "Unexpected request";
                status = 404;
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        Path config = directory.resolve("jobs.json");
        Files.writeString(config, Json.createObjectBuilder()
                .add("url", url).add("repository", "owner/repo")
                .add("jobs", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("name", "build.yml").add("row", 0)))
                .build().toString());
        GitHubActionsCiCdServer server = new GitHubActionsCiCdServer(
                config.toFile(), "test-token", 100, TimeUnit.MILLISECONDS);
        CompletableFuture<List<String>> results = new CompletableFuture<>();
        server.register(job -> results.complete(List.copyOf(job.getResults())));
        http.start();
        try {
            server.initialize();
            assertEquals(List.of("BUILDING", "SUCCESS", "FAILURE", "ABORTED", "FAILURE"),
                    results.get(5, TimeUnit.SECONDS));
            assertTrue(requests.contains("GET /repos/owner/repo/actions/workflows/build.yml/runs?per_page=8"),
                    () -> "Requests: " + requests);
            assertEquals("build.yml", server.getJob(0).getName());
            assertNull(server.getJob(1));

            server.build(server.getJob(0));
            assertEquals("{\"ref\":\"main\"}", dispatchBody.get(), () -> "Requests: " + requests);
            assertEquals("Bearer test-token", authorization.get());
        } finally {
            server.destroy();
            http.stop(0);
        }
    }

    @Test
    void readsRepositoryConfiguration() throws Exception {
        CiCdConfig config = CiCdConfig.fromFile(
                Path.of(getClass().getResource("/github-actions-jobs.json").toURI()).toFile());

        assertEquals("https://api.github.com", config.getUrl());
        assertEquals("lreimer/cicd-launchpad", config.getRepository());
        assertEquals("master", config.getBranch());
        assertNull(config.getUsername());
        assertEquals("gradle.yml", config.getJobs().get(0).getName());
    }

    @Test
    void requiresTokenAndRepository() throws Exception {
        File config = Path.of(getClass().getResource("/github-actions-jobs.json").toURI()).toFile();
        assertThrows(IllegalStateException.class,
                () -> new GitHubActionsCiCdServer(config, null, 1, TimeUnit.SECONDS).initialize());
        assertThrows(IllegalStateException.class,
                () -> new GitHubActionsCiCdServer(config, " ", 1, TimeUnit.SECONDS).initialize());

        Path invalid = directory.resolve("invalid.json");
        Files.writeString(invalid, "{\"repository\":\"no-owner\",\"jobs\":[]}");
        assertThrows(IllegalArgumentException.class,
                () -> new GitHubActionsCiCdServer(invalid.toFile(), "token", 1, TimeUnit.SECONDS).initialize());
    }

    @Test
    void mapsWorkflowRunResults() {
        assertEquals("SUCCESS", GitHubActionsCiCdServer.toResult(run("completed", "success")));
        assertEquals("FAILURE", GitHubActionsCiCdServer.toResult(run("completed", "startup_failure")));
        assertEquals("NOT_BUILT", GitHubActionsCiCdServer.toResult(run("completed", "skipped")));
        assertEquals("BUILDING", GitHubActionsCiCdServer.toResult(run("queued", null)));
        assertEquals("NOT_BUILT", GitHubActionsCiCdServer.toResult(run("completed", null)));
    }

    private static jakarta.json.JsonObject run(String status, String conclusion) {
        var builder = Json.createObjectBuilder().add("status", status);
        if (conclusion == null) {
            builder.addNull("conclusion");
        } else {
            builder.add("conclusion", conclusion);
        }
        return builder.build();
    }
}
