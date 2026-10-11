# CI/CD Launchpad

Visualise and launch CI/CD jobs using a Novation Launchpad Mini MK2.

## Building and Running

Install a JDK 21 and set `JAVA_HOME` to its installation directory. The checked-in
wrapper downloads Gradle 8.14.6 and verifies the distribution's SHA-256 checksum.
Compilation and application execution use the Java 21 toolchain.

```bash
$ ./gradlew clean build
$ ./gradlew test
$ ./gradlew run --args="--help"
```

### GitHub Actions (default)

GitHub Actions is the default CI/CD server; `-g` (or `--github`) selects it
explicitly. Authentication uses a GitHub personal access token read from the
`GITHUB_TOKEN` environment variable. A fine-grained token needs the repository
permission *Actions: Read and write* (read-only suffices if you do not trigger
builds); a classic token needs the `repo` (or `public_repo`) and `workflow` scopes.

```bash
$ export GITHUB_TOKEN=<your personal access token>
$ ./gradlew run --args="-f src/test/resources/github-actions-jobs.json"
```

The configuration names the repository and the workflows to show. Job names are
workflow file names (or workflow IDs); `url` defaults to `https://api.github.com`
and may point to a GitHub Enterprise API. If `branch` is set, only runs for that
branch are shown and builds are dispatched on it; otherwise all runs are shown and
builds use the repository's default branch.

```json
{
  "url": "https://api.github.com",
  "repository": "lreimer/cicd-launchpad",
  "branch": "master",
  "jobs": [ { "row": 0, "name": "gradle.yml" } ]
}
```

Each row shows the latest eight workflow runs, newest first: successful runs are
steady green, failed, timed-out or startup-failure runs are red, and actively
running jobs blink green (alternating bright green and off). Queued, waiting,
cancelled or skipped runs are bright yellow. All other statuses also display
bright yellow; pads without run history remain unlit.
Pressing a row's button triggers the
workflow through a `workflow_dispatch` event, so the workflow must declare the
`workflow_dispatch` trigger.

#### Launchpad demo workflows

The example configuration in `src/test/resources/github-actions-jobs.json` keeps
the real Gradle build on row 0 and maps seven dummy workflows to rows 1-7
(zero-based row indices). These demos run **only on manual dispatch**, not on
pushes or pull requests, so their intentional failures do not affect normal CI.
They use shell commands without checkout or build dependencies.

| Row | Workflow file | Behavior | Launchpad display |
| --- | --- | --- | --- |
| 1 | `demo-success.yml` | Immediately succeeds | Bright green |
| 2 | `demo-failure.yml` | Immediately fails intentionally | Bright red |
| 3 | `demo-skipped.yml` | Skips its only job | Bright yellow |
| 4 | `demo-cancelled.yml` | Cancels its own run through the GitHub API | Bright yellow |
| 5 | `demo-timeout.yml` | Sleeps past a one-minute job timeout | Blinking green while running, then bright red |
| 6 | `demo-alternating.yml` | Odd run numbers fail; even run numbers succeed | Alternating red/green history after repeated dispatches |
| 7 | `demo-running.yml` | Sleeps for 30 seconds, then succeeds | Blinking green while running, then steady bright green |

Yellow represents pending runs and completed results other than success or
failure. Running jobs use the Launchpad's native automatic flashing. A new run shifts older
results along the row; dispatch each workflow eight times to fill its history.
Re-running an existing run keeps its run number, so use new dispatches for the
alternating demo.

After these files are merged into the default branch (`master`), trigger them
from the GitHub Actions UI, the Launchpad build buttons, or the GitHub CLI:

```bash
$ gh workflow run demo-success.yml --repo lreimer/cicd-launchpad --ref master
$ gh workflow run demo-alternating.yml --repo lreimer/cicd-launchpad --ref master
$ gh run list --repo lreimer/cicd-launchpad --workflow demo-alternating.yml
```

Dispatch the alternating workflow repeatedly to mix red and green pads. The
running demo provides a short window to observe the blinking green in-progress
display; runner queue time and the application's polling interval affect when
changes appear. The cancellation demo alone grants its built-in `GITHUB_TOKEN`
the `actions: write` permission; the other demos grant no token permissions.
Repository or organization policies must allow that permission for self-cancellation.

### Jenkins

Use `-j` (or `--jenkins`) to visualise Jenkins jobs instead. `-g` and `-j` are
mutually exclusive. The username and password options only apply to Jenkins.

```bash
$ docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:lts

$ ./gradlew run --args="-j -f src/test/resources/jenkins-jobs.json"
$ ./gradlew run --args="-j -u admin -p admin -f src/test/resources/jenkins-jobs.json"
```

`build/distributions` contains the application ZIP and TAR distributions. They
require Java 21 to run. Runtime dependency JARs are also copied to
`build/runtime-libs`; picocli's code generator is a build-only dependency and is
not shipped with the application.

Dependencies are resolved from Maven Central. JSON configuration uses Jakarta
JSON-P with Parsson, logging uses SLF4J 2, and the wave screensaver uses Commons
Collections 4. The Jenkins client remains at 0.3.8 (its latest published version),
with compatible updates to Jackson 2, Guava, Apache HttpComponents 4, dom4j,
Jaxen, and its Commons dependencies. Its legacy JSON-lib, EZMorph, Commons Lang 2,
and Commons Collections 3 dependencies remain necessary; fully removing them
requires replacing the Jenkins client. ORO also remains at its latest release.

Tests cover CLI parsing, JSON configuration, Jenkins polling/build triggering
and GitHub Actions polling/dispatching against local HTTP stubs, and wave
rendering without requiring Jenkins, GitHub or MIDI hardware.

### Optional GraalVM native image

Normal JVM execution does not require GraalVM. To collect native-image metadata,
use a GraalVM JDK 21 with the agent installed. Set `JAVA_HOME` to that GraalVM
installation and prepend `$JAVA_HOME/bin` to `PATH` in the same shell that runs
Gradle; merely installing GraalVM does not change the selected Java runtime:

```bash
$ export JAVA_HOME=/path/to/graalvm-jdk-21
$ export PATH="$JAVA_HOME/bin:$PATH"
$ java -version
$ native-image --version
```

Then collect metadata and build:

```bash
$ ./gradlew run -PnativeImageAgent --args="-f src/test/resources/github-actions-jobs.json"
$ ./gradlew run -PnativeImageAgent --args="-j -f src/test/resources/jenkins-jobs.json"
$ ./gradlew reflectionConfigGenerator
$ ./gradlew graalNativeImage
```

`graalNativeImage` requires GraalVM 21's `native-image` executable on `PATH`.
It uses the resolved runtime classpath and generates `build/cicd-launchpad`
without a fallback JVM image. The old Java 8 fallback-image flags and separate
EC crypto library copy task are no longer used. `copyJsound` reads native sound
libraries from the Java 21 toolchain's `lib` directory.

The checked-in JNI/reflection metadata originated from the older runtime.
Regenerate and verify it with the agent and your Launchpad hardware before
relying on native execution. Agent output is merged into
`build/resources/main/META-INF/native-image`; preserve any required updates in
`src/main/resources/META-INF/native-image` before a clean build.

The native build and CLI help/version/error-exit smoke tests have been verified
with GraalVM CE 21.0.2 on macOS ARM64. The metadata agent also loads successfully.
Native startup reaches MIDI initialization, but full device operation requires
an attached Launchpad; without it, startup reports `MidiUnavailableException`.
Live Jenkins polling and build triggering through the native executable remain
unverified (the JVM tests exercise them against a local HTTP stub).

## Maintainer

M.-Leander Reimer (@lreimer), <mario-leander.reimer@qaware.de>

## License

This software is provided under the MIT open source license, read the `LICENSE`
file for details.
