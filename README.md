# CI/CD Launchpad

Visualise and launch CI/CD jobs using a Novation Launchpad Mini MK2.

## Building and Running

```bash
$ ./gradlew clean ass

$ docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:lts
$ ./gradlew run --args="-f src/test/resources/jenkins-jobs.json"
```

## Maintainer

M.-Leander Reimer (@lreimer), <mario-leander.reimer@qaware.de>

## License

This software is provided under the MIT open source license, read the `LICENSE`
file for details.
