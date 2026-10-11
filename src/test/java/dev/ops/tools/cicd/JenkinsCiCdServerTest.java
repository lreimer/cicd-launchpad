package dev.ops.tools.cicd;

import com.sun.net.httpserver.HttpServer;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JenkinsCiCdServerTest {

    @TempDir
    Path directory;

    @Test
    void pollsBuildResultsAndTriggersBuildWithUpgradedDependencies() throws Exception {
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String url = "http://127.0.0.1:" + http.getAddress().getPort() + "/";
        AtomicInteger triggers = new AtomicInteger();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> crumb = new AtomicReference<>();
        List<String> requests = new CopyOnWriteArrayList<>();
        http.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.add(exchange.getRequestMethod() + " " + path);
            String response;
            int status = 200;
            if (path.equals("/job/demo/api/json")) {
                response = """
                        {"name":"demo","url":"%sjob/demo/","builds":[
                          {"number":2,"url":"%sjob/demo/2/"},
                          {"number":1,"url":"%sjob/demo/1/"}]}
                        """.formatted(url, url, url);
            } else if (path.equals("/job/demo/2/api/json")) {
                response = "{\"number\":2,\"result\":\"SUCCESS\",\"building\":false}";
            } else if (path.equals("/job/demo/1/api/json")) {
                response = "{\"number\":1,\"result\":\"FAILURE\",\"building\":false}";
            } else if (path.equals("/crumbIssuer/api/json")) {
                response = "{\"crumbRequestField\":\"Jenkins-Crumb\",\"crumb\":\"test-crumb\"}";
            } else if (path.equals("/job/demo/build/api/json") && exchange.getRequestMethod().equals("POST")) {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                crumb.set(exchange.getRequestHeaders().getFirst("Jenkins-Crumb"));
                triggers.incrementAndGet();
                exchange.getResponseHeaders().set("Location", url + "queue/item/1/");
                response = "";
                status = 201;
            } else {
                response = "Unexpected request: " + exchange.getRequestMethod() + " " + path;
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
                .add("url", url).add("username", "file-user").add("password", "file-token")
                .add("jobs", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("name", "demo").add("row", 0)))
                .build().toString());
        JenkinsCiCdServer server = new JenkinsCiCdServer(
                config.toFile(), "cli-user", "cli-token", 100, TimeUnit.MILLISECONDS);
        CompletableFuture<List<String>> results = new CompletableFuture<>();
        server.register(job -> results.complete(List.copyOf(job.getResults())));
        http.start();
        try {
            server.initialize();
            assertEquals(List.of("SUCCESS", "FAILURE"), results.get(5, TimeUnit.SECONDS));
            assertEquals("demo", server.getJob(0).getName());
            server.build(server.getJob(0));
            assertEquals(1, triggers.get(), () -> "Requests: " + requests);
            assertEquals("Basic " + Base64.getEncoder().encodeToString(
                    "cli-user:cli-token".getBytes(StandardCharsets.UTF_8)), authorization.get());
            assertEquals("test-crumb", crumb.get());
        } finally {
            server.destroy();
            http.stop(0);
        }
    }
}
