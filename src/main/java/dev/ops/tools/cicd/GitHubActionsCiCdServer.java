package dev.ops.tools.cicd;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The CiCdServer implementation for GitHub Actions. Each configured job name is a workflow
 * file name (e.g. {@code gradle.yml}) or workflow ID of the configured repository.
 * Authentication uses a GitHub personal access token.
 */
public class GitHubActionsCiCdServer implements CiCdServer {

    public static final String TOKEN_ENV_VARIABLE = "GITHUB_TOKEN";
    public static final String DEFAULT_API_URL = "https://api.github.com";

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubActionsCiCdServer.class);
    private static final int MAX_RESULTS = 8;

    private final File configFile;
    private final String token;
    private final long time;
    private final TimeUnit unit;
    private final Set<Consumer<CiCdJob>> callbacks;

    private CiCdConfig config;
    private String apiUrl;
    private HttpClient http;
    private ScheduledExecutorService executorService;

    public GitHubActionsCiCdServer(File configFile, String token, long time, TimeUnit unit) {
        this.configFile = configFile;
        this.token = token;
        this.time = time;
        this.unit = unit;
        this.callbacks = new CopyOnWriteArraySet<>();
    }

    @Override
    public void initialize() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing GitHub personal access token. Set the "
                    + TOKEN_ENV_VARIABLE + " environment variable.");
        }

        config = CiCdConfig.fromFile(configFile);
        if (config.getRepository() == null || !config.getRepository().matches("[^/\\s]+/[^/\\s]+")) {
            throw new IllegalArgumentException("Invalid or missing GitHub repository (expected 'owner/repo').");
        }
        apiUrl = stripTrailingSlash(config.getUrl() == null ? DEFAULT_API_URL : config.getUrl());

        LOGGER.info("Using {}", config);

        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        LOGGER.info("Connected to GitHub Actions API {} for repository {}", apiUrl, config.getRepository());

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(this::update, time, time, unit);
    }

    @Override
    public void register(Consumer<CiCdJob> callback) {
        callbacks.add(callback);
    }

    private void notify(CiCdJob job) {
        for (Consumer<CiCdJob> c : callbacks) {
            c.accept(job);
        }
    }

    private void update() {
        LOGGER.info("Updating CI/CD jobs ...");
        for (CiCdJob job : config.getJobs()) {
            try {
                String path = workflowPath(job) + "/runs?per_page=" + MAX_RESULTS + branchQuery();
                HttpResponse<String> response = send(get(path));
                if (response.statusCode() != 200) {
                    LOGGER.warn("Unable to get workflow runs for Job {} (HTTP {})", job.getName(), response.statusCode());
                    continue;
                }

                JsonArray runs = parse(response.body()).getJsonArray("workflow_runs");
                LOGGER.info("   - {}", job.getName());

                job.getResults().clear();
                for (int i = 0; i < runs.size() && i < MAX_RESULTS; i++) {
                    job.addResult(toResult(runs.getJsonObject(i)));
                }
                notify(job);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Unable to get workflow runs for Job {}.", job.getName(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Maps a GitHub workflow run to a Jenkins style result name used by the LaunchpadColor.
     *
     * @param run the workflow run JSON
     * @return the result name
     */
    static String toResult(JsonObject run) {
        if (!"completed".equals(run.getString("status", null))) {
            return "in_progress".equals(run.getString("status", null)) ? "BUILDING" : "NOT_BUILT";
        }
        JsonValue conclusion = run.get("conclusion");
        if (conclusion == null || conclusion.getValueType() != JsonValue.ValueType.STRING) {
            return "NOT_BUILT";
        }
        switch (run.getString("conclusion")) {
            case "success":
                return "SUCCESS";
            case "failure":
            case "timed_out":
            case "startup_failure":
                return "FAILURE";
            case "cancelled":
                return "ABORTED";
            default:
                return "NOT_BUILT";
        }
    }

    @Override
    public void destroy() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (http != null) {
            http.close();
        }
    }

    @Override
    public CiCdJob getJob(int index) {
        if (index < 0 || index >= config.getJobs().size()) {
            return null;
        } else {
            return config.getJobs().get(index);
        }
    }

    @Override
    public void build(CiCdJob job) {
        if (job == null) {
            return;
        }

        LOGGER.info("Trigger build for Job {}", job.getName());

        try {
            String ref = config.getBranch() != null ? config.getBranch() : defaultBranch();
            String body = Json.createObjectBuilder().add("ref", ref).build().toString();
            HttpRequest request = request(workflowPath(job) + "/dispatches")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = send(request);
            if (response.statusCode() / 100 != 2) {
                LOGGER.warn("Unable to trigger build for Job {} on ref {} (HTTP {}). "
                                + "Does the workflow define a workflow_dispatch trigger? {}",
                        job.getName(), ref, response.statusCode(), response.body());
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Unable to trigger build for Job {}", job.getName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String defaultBranch() throws IOException, InterruptedException {
        HttpResponse<String> response = send(get("/repos/" + config.getRepository()));
        if (response.statusCode() != 200) {
            throw new IOException("Unable to get repository " + config.getRepository()
                    + " (HTTP " + response.statusCode() + ")");
        }
        return parse(response.body()).getString("default_branch");
    }

    private String workflowPath(CiCdJob job) {
        return "/repos/" + config.getRepository() + "/actions/workflows/" + encode(job.getName());
    }

    private String branchQuery() {
        return config.getBranch() == null ? "" : "&branch=" + encode(config.getBranch());
    }

    private HttpRequest get(String path) {
        return request(path).GET().build();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Authorization", "Bearer " + token);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static JsonObject parse(String body) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            return reader.readObject();
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
