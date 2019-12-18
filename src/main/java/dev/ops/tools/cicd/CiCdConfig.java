package dev.ops.tools.cicd;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
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
    private List<CiCdJob> jobs = new ArrayList<>(8);

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public URI getURI() {
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

            config.setUrl(configObject.getString("url"));
            config.setUsername(configObject.getString("username"));
            config.setPassword(configObject.getString("password"));

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
                .add("jobs=" + jobs)
                .toString();
    }
}
