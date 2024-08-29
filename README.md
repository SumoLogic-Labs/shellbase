# shellbase
Sumo Logic's Scala-based interactive shell framework

## Install / Download
These are the components we provide:
* `shellbase-core_2.13` contains everything you need to make a basic, working shell
* `shellbase-slack_2.13` contains the necessary pieces to post your commands to Slack.

```
    <dependency>
      <groupId>com.sumologic.shellbase</groupId>
      <artifactId>shellbase-core_2.13</artifactId>
      <version>4.0.0</version>
    </dependency>

    <dependency>
      <groupId>com.sumologic.shellbase</groupId>
      <artifactId>shellbase-slack_2.13</artifactId>
      <version>4.0.0</version>
    </dependency>
```

## Built-In Commands

* **help** / **?** - Displays a summary of available commands.
* **clear** - Clears the text on the screen
* **exit** / **quit** - Exits the current shell
* **sleep** / **zzz** - Sleeps for the specified duration.  Can use compact time: `5m` means `5 minutes`
* **echo** - Writes output to the screen (STDOUT)
* **tee** - Lets you fork stdout to a file
* **time** - Time how long a command took
* **run_script** / **script** - Execute the specified script.  Will attempt to be smart about locating

## Example Shell

We've put together a very simple [example shell](./shellbase-example) that can generate random numbers.  

## [Dev] Building/testing locally

To build project in default Scala version:
```
./gradlew build
```

To build project in any supported Scala version:
```
./gradlew build -PscalaVersion=2.13.10
```

For testing, change your consumer `pom.xml` or `gradle.properties` to depend on the `SNAPSHOT` version generated.
Make sure, your consumer can resolve artifacts from a local repository.

### [Dev] Managing Scala versions

This project supports multiple versions of Scala. Supported versions are listed in `gradle.properties`.
- `supportedScalaVersions` - list of supported versions (Gradle prevents building with versions from 
outside this list)
- `defaultScalaVersion` - default version of Scala used for building - can be overridden with `-PscalaVersion`

### [Dev] How to release new version
1. Make sure you have all credentials.
    1. Can login as `sumoapi` https://oss.sonatype.org/index.html
    2. Can import and verify the signing key:
        ```
        gpg --import ~/Desktop/api.private.key
        gpg-agent --daemon
        touch a
        gpg --use-agent --sign a
        gpg -k
        ```
    3. Have nexus and signing credentials in `~/.gradle/gradle.properties`
        ```
        nexus_username=sumoapi
        nexus_password=${sumoapi_password_for_sonatype_nexus}
        signing.gnupg.executable=gpg
        signing.gnupg.keyName=${id_of_imported_sumoapi_key}
        signing.gnupg.passphrase=${password_for_imported_sumoapi_key}
        ```
2. Remove `-SNAPSHOT` suffix from `version` in `build.gradle`
3. Make a release branch with Scala version and project version, ex. `shellbase-5.0.1`:
    ```
    export RELEASE_VERSION=shellbase-5.0.1
    git checkout -b ${RELEASE_VERSION}
    git add build.gradle
    git commit -m "[release] ${RELEASE_VERSION}"
    ```
4. Perform a release in selected Scala versions:
    ```
    ./gradlew shellbase-example:publish -PscalaVersion=2.12.19
    ./gradlew shellbase-slack:publish -PscalaVersion=2.12.19
    ./gradlew shellbase-core:publish -PscalaVersion=2.12.19
    ./gradlew shellbase-example:publish -PscalaVersion=2.13.14
    ./gradlew shellbase-slack:publish -PscalaVersion=2.13.14
    ./gradlew shellbase-core:publish -PscalaVersion=2.13.14

    ```
5. Go to https://oss.sonatype.org/index.html#stagingRepositories, search for com.sumologic, close and release your repo. 
NOTE: If you had to login, reload the URL. It doesn't take you to the right page post-login
6. Update the `README.md` and `CHANGELOG.md` with the new version and set upcoming snapshot `version` 
in `build.gradle`, ex. `5.0.2-SNAPSHOT`
7. Commit the change and push as a PR:
    ```
    git add build.gradle README.md CHANGELOG.md
    git commit -m "[release] Updating version after release ${RELEASE_VERSION}"
    git push
    ```
