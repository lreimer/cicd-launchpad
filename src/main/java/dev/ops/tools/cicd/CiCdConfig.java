package dev.ops.tools.cicd;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * The main CI/CD configuration.
 */
public class CiCdConfig {

    private String url;
    private String username;
    private String password;
    private String repository;
    private String branch;
    private List<CiCdJob> jobs = new ArrayList<>(8);

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public URI getURI() {
        if (url == null) {
            throw new IllegalArgumentException("Missing CI/CD server URL.");
        }
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid CI/CD server URL.", e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public List<CiCdJob> getJobs() {
        return jobs;
    }

    public void setJobs(List<CiCdJob> jobs) {
        if (jobs.size() > 8) {
            throw new IllegalArgumentException("Too many jobs configured.");
        }
        this.jobs = jobs;
    }

    public static CiCdConfig fromFile(File file) {
        CiCdConfig config = new CiCdConfig();
        try (JsonReader reader = Json.createReader(new FileInputStream(file))) {
            JsonObject configObject = reader.readObject();

            config.setUrl(configObject.getString("url", null));
            config.setUsername(configObject.getString("username", null));
            config.setPassword(configObject.getString("password", null));
            config.setRepository(configObject.getString("repository", null));
            config.setBranch(configObject.getString("branch", null));

            config.setJobs(CiCdJob.fromJson(configObject));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading JSON config file.", e);
        }
        return config;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CiCdConfig.class.getSimpleName() + "[", "]")
                .add("url='" + url + "'")
                .add("username='" + username + "'")
                .add("password='" + password + "'")
                .add("repository='" + repository + "'")
                .add("branch='" + branch + "'")
                .add("jobs=" + jobs)
                .toString();
    }
}
