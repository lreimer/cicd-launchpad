# Repository instructions

## Build, test, and run

Use the Gradle wrapper:

```bash
./gradlew clean build
./gradlew test
./gradlew test --tests 'dev.ops.tools.ExampleTest'
./gradlew run --args='-f src/test/resources/jenkins-jobs.json'
```

There are currently no test classes or test-framework dependencies; the filtered test command shows how to run one after tests are added. No lint task or lint plugin is configured. The wrapper pins Gradle 5.2.1 and the project targets Java 8; this older Gradle version may fail on newer JDKs.

## Architecture

This is a Java CLI application that connects Jenkins job status to a Novation Launchpad Mini MK2. `CiCdLaunchpad` parses CLI options with picocli, loads a `CiCdServer`, and wires it to `CiCdLaunchpadController`; the controller coordinates server updates and MIDI input/output.

`JenkinsCiCdServer` reads the JSON configuration, periodically polls the configured jobs, and sends updated `CiCdJob` objects through registered callbacks. The controller renders the latest eight build results in a job's Launchpad row. `LaunchpadColor` maps Jenkins results to device colors. `MidiSystemHandler` acquires Java Sound MIDI endpoints, while `LaunchpadDevice` translates grid positions and colors into MIDI messages. Top-row MIDI buttons trigger job builds; numbered buttons toggle the `Screensaver` implementations.

## Repository-specific conventions

- The JSON config has `url`, `username`, `password`, and a `jobs` array. Each job has a `name` and a `row` from 0 through 7; at most eight jobs are supported. CLI username/password options override the values in the file.
- Keep job-result rendering aligned with `LaunchpadColor.forResult`; Jenkins results other than `SUCCESS`, `UNSTABLE`, and `FAILURE` currently render as unlit.
- New CI/CD server implementations should follow `CiCdServer`'s lifecycle and callback contract; the current CLI factory selects Jenkins.
- Native-image configuration lives in `src/main/resources/META-INF/native-image`. Update its reflection, JNI, proxy, or resource metadata when corresponding runtime/native-image requirements change.
