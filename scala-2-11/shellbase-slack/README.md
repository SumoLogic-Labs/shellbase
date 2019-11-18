# shellbase-slack
Provides Slack integration for shells.

## Posting Commands to Slack Automatically
One of the most useful things we do with our shells at Sumo Logic is to have specific commands print to Slack when they're executed.  This helps communicate the operator's actions to everyone.  You can do this in a fairly simple manner by mixing in the `PostCommandToSlack` to your classes and providing a `SlackState` object:

```
val apiKey: String = ???

commands += SomeShellCommand with PostCommandToSlack {
  protected val slackState: SlackState = new SlackState {
    def slackClient: Option[SlackClient] = Some(new SlackClient(apiKey))
    def slackChannel: Option[String] = Some("operations")
  }
}
```

Now, pasting this everywhere is a hassle, so we find it best to create a trait that extends `PostCommandToSlack` and mix that in instead:

```
trait PostCommandToSlackWithCredentials extends PostCommandToSlack {
  private val apiKey: String = ???

  protected val slackState: SlackState = new SlackState {
    def slackClient: Option[SlackClient] = Some(new SlackClient(apiKey))
    def slackChannel: Option[String] = Some("operations")
  }
}

commands += SomeShellCommand with PostCommandToSlackWithCredentials
```
