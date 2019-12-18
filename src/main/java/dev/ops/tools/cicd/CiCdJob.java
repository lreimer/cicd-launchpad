package dev.ops.tools.cicd;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * The CI/CD job configuration.
 */
public class CiCdJob {
    private int row;
    private String name;

    private List<String> results = new ArrayList<>(8);

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        if (row < 0 || row > 7) {
            throw new IllegalArgumentException("Invalid row " + row);
        }
        this.row = row;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getResults() {
        return results;
    }

    public void setResults(List<String> results) {
        this.results = results;
    }

    public void addResult(String result) {
        this.results.add(result);
    }

    public String getResult(int index) {
        return this.results.get(index);
    }

    /**
     * Return a list of deserialized CiCdJob instances for given JSON config.
     *
     * @param configObject the JSON config
     * @return list of {@link CiCdJob}
     */
    public static List<CiCdJob> fromJson(JsonObject configObject) {
        JsonArray jobs = configObject.getJsonArray("jobs");
        return jobs.getValuesAs((Function<JsonObject, CiCdJob>) jsonObject -> {
            CiCdJob job = new CiCdJob();
            job.setRow(jsonObject.getInt("row"));
            job.setName(jsonObject.getString("name"));
            return job;
        });
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CiCdJob.class.getSimpleName() + "[", "]")
                .add("row=" + row)
                .add("name='" + name + "'")
                .toString();
    }
}
