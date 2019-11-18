[![Build Status](https://travis-ci.org/SumoLogic/shellbase.svg?branch=master)](https://travis-ci.org/SumoLogic/shellbase)
[![codecov.io](https://codecov.io/github/SumoLogic/shellbase/coverage.svg?branch=master)](https://codecov.io/github/SumoLogic/shellbase?branch=master)
[![Join the chat at https://gitter.im/SumoLogic/shellbase](https://badges.gitter.im/SumoLogic/shellbase.svg)](https://gitter.im/SumoLogic/shellbase?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# shellbase
Sumo Logic's Scala-based interactive shell framework

## Install / Download
These are the components we provide:
* `shellbase-core` contains everything you need to make a basic, working shell
* `shellbase-slack` contains the necessary pieces to post your commands to Slack.

```
    <dependency>
      <groupId>com.sumologic.shellbase</groupId>
      <artifactId>shellbase-core</artifactId>
      <version>1.5.1</version>
    </dependency>

    <dependency>
      <groupId>com.sumologic.shellbase</groupId>
      <artifactId>shellbase-slack</artifactId>
      <version>1.5.1</version>
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

1. Locally compile/install skipping the PGP signatures: 

   ```
   ./mvnw clean install -Dgpg.skip=true
   ```
1. Change your consumer `pom.xml` to depend on the `SNAPSHOT` version generated.

## [Dev] How to release new version

1. Make sure you have all credentials.
  * Can login as `sumoapi` https://oss.sonatype.org/index.html
  * Have nexus credentials ~/.m2/settings.xml

  ```
  <server>
    <username>sumoapi</username>
    <password>****</password>
    <id>sonatype-nexus-staging</id>
  </server>
  ```
  * Signing key:

  ```
    gpg --import ~/Desktop/api.private.key
    gpg-agent --daemon
    touch a
    gpg --use-agent --sign a

  ```
2. `./mvnw release:prepare`
3. `git clean -i` and remove untracked files, besides release.properties
4. `./mvnw release:perform` (alternative `git checkout HEAD~1 && ./mvnw deploy`)
5. Go to https://oss.sonatype.org/index.html#stagingRepositories, search for com.sumologic and release your repo. NOTE: If you had to login, reload the URL.  It doesn't take you to the right page post-login
6. Update the README.md file with the new version and commit the change
7. Push your commits as PR (`git push origin master:new-branch`)
