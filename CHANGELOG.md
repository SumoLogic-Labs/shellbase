# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

Nothing right now.  We plan to drop support for Scala 2.11 at a future time.

## [2.2.0] - 2022-02-01
### Changed
- SlackState.userNameToBeUsedWhenPosting introduced in favor of slackOptions (#64)

## [2.1.1] - 2022-01-24
### Changed
- Upgraded Gradle to 7.2
- Removed slf4j-log4j dependency
- Scala versions upgraded

## [2.1.0] - 2021-04-15
### Added
- Added back support for Slack Attachments and Blocks

## [2.0.0] - 2021-03-25
### Added
- Support for Scala 2.13

### Changed
- We have replaced flyberry's slack client with `slack-scala-client`.  This introduced a few breaking changes. 
  - `sendSlackMessageIfConfigured` now returns `thread_ts` (as a `String`) instead of `PostMessageResponse`
  - `SlackState.slackClient` returns `Option[BlockingSlackApiClient]` instead of `Option[SlackClient]`
  - `SlackState.actorSystem` is now required to execute commands.
- Upgraded Gradle tooling to 6.6.1 from 5.5.1
- Many libraries were upgraded to newer versions, including Scala 2.11 and 2.12
- `PostToSlackHelper.blacklistedUsernames` has been renamed to `excludedUsernames`
- `ShellPrompter.askQuestion` and `pickFromOptions` now use `Option` instead of `null`.

### Removed
- Dropped support for building with Maven, making Gradle the defacto tool


## [1.5.4] - 2020-02-17
### Changed
- Effectively reverting to 1.5.2

## [1.5.3] - 2020-01-23
BROKEN

### Changed
- no exit if the exit code is 0 (#46)

## [1.5.2] - 2019-11-20

### Added
- Multiple Scala Version support: `2.11`, `2.12`
- Releasing new versions using Gradle

### Changed
- Updated Gradle configuration as a primary build tool
- Artifact id has a Scala version suffix
