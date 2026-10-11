# Repository instructions

## Build, test, and run

Use the Gradle wrapper:

```bash
./gradlew clean build
./gradlew test
./gradlew test --tests 'dev.ops.tools.ExampleTest'
./gradlew run --args='-f src/test/resources/jenkins-jobs.json'
```

Tests use JUnit Jupiter and include local HTTP stubs for Jenkins and the GitHub Actions API; Jenkins, GitHub and MIDI hardware are not required. No lint task or lint plugin is configured. The wrapper pins Gradle 8.14.6 and the project uses a Java 21 toolchain with `--release 21`. Set `JAVA_HOME` to a JDK 21 installation.

## Architecture

This is a Java CLI application that connects Jenkins job or GitHub Actions workflow status to a Novation Launchpad Mini MK2. `CiCdLaunchpad` parses CLI options with picocli, loads a `CiCdServer`, and wires it to `CiCdLaunchpadController`; the controller coordinates server updates and MIDI input/output.

`JenkinsCiCdServer` reads the JSON configuration, periodically polls the configured jobs, and sends updated `CiCdJob` objects through registered callbacks. `GitHubActionsCiCdServer` does the same for GitHub Actions workflow runs using the JDK `HttpClient`, mapping run status/conclusion to Jenkins-style result names, and triggers builds via `workflow_dispatch`. The controller renders the latest eight build results in a job's Launchpad row. `LaunchpadColor` maps Jenkins results to device colors. `MidiSystemHandler` acquires Java Sound MIDI endpoints, while `LaunchpadDevice` translates grid positions and colors into MIDI messages. Top-row MIDI buttons trigger job builds; numbered buttons toggle the `Screensaver` implementations.

## Repository-specific conventions

- The Jenkins JSON config has `url`, `username`, `password`, and a `jobs` array. The GitHub Actions config has `repository` (`owner/repo`), optional `url` (API base, default `https://api.github.com`), optional `branch`, and a `jobs` array whose names are workflow file names or IDs; its token comes only from the `GITHUB_TOKEN` environment variable. Each job has a `name` and a `row` from 0 through 7; at most eight jobs are supported. CLI username/password options override the values in the file.
- Keep job-result rendering aligned with `LaunchpadColor.forResult`; Jenkins results other than `SUCCESS`, `UNSTABLE`, and `FAILURE` currently render as unlit.
- New CI/CD server implementations should follow `CiCdServer`'s lifecycle and callback contract; the CLI selects Jenkins by default and GitHub Actions with `-g/--github`.
- Native-image configuration lives in `src/main/resources/META-INF/native-image`. Update its reflection, JNI, proxy, or resource metadata when corresponding runtime/native-image requirements change.
