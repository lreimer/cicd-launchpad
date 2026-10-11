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

$ docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:lts

$ ./gradlew run --args="-f src/test/resources/jenkins-jobs.json"
$ ./gradlew run --args="-u admin -p admin -f src/test/resources/jenkins-jobs.json"
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
against a local HTTP stub, and wave rendering without requiring Jenkins or MIDI
hardware.

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
$ ./gradlew run -PnativeImageAgent --args="-f src/test/resources/jenkins-jobs.json"
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
