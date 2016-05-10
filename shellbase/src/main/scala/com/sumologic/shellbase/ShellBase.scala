package com.sumologic.shellbase

import java.io.{File, FileOutputStream, PrintStream}
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import com.sumologic.shellbase.cmdline.RichCommandLine._
import com.sumologic.shellbase.cmdline.{CommandLineArgument, CommandLineFlag, CommandLineOption}
import com.sumologic.shellbase.interrupts.{InterruptKeyMonitor, KillableSingleThread}
import com.sumologic.shellbase.notifications.{InMemoryShellNotificationManager, NotificationCommandSet, RingingNotification, ShellNotificationManager}
import com.sumologic.shellbase.timeutil.{TimeFormats, TimedBlock}
import jline.console.ConsoleReader
import jline.console.completer.{ArgumentCompleter, Completer, NullCompleter, StringsCompleter}
import jline.console.history.FileHistory
import org.apache.commons.cli.{CommandLine, GnuParser, HelpFormatter, Options, ParseException, Option => CLIOption}
import org.apache.commons.io.output.TeeOutputStream
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

/**
  * A shell base that can be used to build shell like applications.
  */
abstract class ShellBase(val name: String) {

  // -----------------------------------------------------------------------------------------------
  // Abstract methods to implement.
  // -----------------------------------------------------------------------------------------------

  /**
    * Return the list of commands.
    */
  def commands: Seq[ShellCommand] = List[ShellCommand]()

  /**
    * Return additional command line options.
    */
  def additionalOptions: Seq[CLIOption] = List[CLIOption]()

  /**
    * Process additional command line options.
    */
  def init(cmdLine: CommandLine): Boolean = true

  /**
    * Override for a custom prompt.
    */
  def prompt: String = "$"

  /**
    * Return a custom banner.
    */
  def banner: String = ""

  /**
    * Where to look for scripts.
    */
  def scriptDir: File = new File("scripts/")

  /**
    * File extension for scripts.
    */
  def scriptExtension: String = name

  /**
    * Prints commands to stdout as they're executed
    */
  def verboseMode: Boolean = false

  /**
    * Name of the history file.
    */
  def historyPath: File = new File("%s/.%s_history".format(System.getProperty("user.home"), name))

  /**
    * Manages notifications
    */
  lazy val notificationManager: ShellNotificationManager = new InMemoryShellNotificationManager(Seq(new RingingNotification))

  /**
    * Pre, post command hooks
    */
  protected[this] def preCommandHooks: Seq[ShellCommandSet.ExecuteHook] = List()

  protected[this] def postCommandHooks: Seq[ShellCommandSet.ExecuteHook] = List()

  private val _logger = LoggerFactory.getLogger(getClass)

  /**
    * Exit the shell.
    */
  protected def exitShell(exitValue: Int) {
    System.exit(exitValue)
  }

  // rootSet is visible for testing only
  private[shellbase] val rootSet = new ShellCommandSet(name, "")
  rootSet.preExecuteHooks.appendAll(preCommandHooks)
  rootSet.postExecuteHooks.appendAll(postCommandHooks)

  private val history = new FileHistory(historyPath)
  history.setMaxSize(1000)

  // NOTE(stefan, 2014-01-06): From the jline JavaDoc:Implementers should install shutdown hook to
  // call {@link FileHistory#flush} to save history to disk.
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = {
      history.flush()
      interruptKeyMonitor.shutdown()
    }
  })

  private val interruptKeyMonitor = new InterruptKeyMonitor()
  interruptKeyMonitor.init()

  private val reader = new ConsoleReader()
  reader.setHistory(history)
  reader.setHandleUserInterrupt(false)

  def main(args: Array[String]) = {
    Thread.currentThread.setName("Shell main")
    try {
      val result = actualMain(args)
      exitShell(result)
    } finally {
      interruptKeyMonitor.shutdown()
    }
  }

  def actualMain(args: Array[String]): Int = {

    val options = new Options
    for (optn <- additionalOptions) {
      options.addOption(optn)
    }

    options.addOption(null, "no-exit", false,
      "Don't exit after executing the command passed on the command line.")

    val cmdLine: CommandLine = try {
      new GnuParser().parse(options, args, true)
    } catch {
      case e: ParseException =>
        println(e.getMessage)
        new HelpFormatter().printHelp(name, options)
        return 1
    }

    if (init(cmdLine)) {
      initializeCommands()

      val arguments = cmdLine.getArgs
      if (arguments.nonEmpty) {
        val interactiveAfterScript = cmdLine.hasOption("no-exit")

        if (interactiveAfterScript) {
          reader.clearScreen
          println(banner)
        }

        val scriptSucceeded = rootSet.executeLine(List[String]() ++ arguments)

        if (!interactiveAfterScript && !scriptSucceeded) {
          println(s"Execution failed! Input was ${arguments.mkString(" ")}")
          return 1
        }

        if (interactiveAfterScript) {
          interactiveMainLoop()
        }
      } else {
        reader.clearScreen

        println(banner)

        interactiveMainLoop()
      }

      0
    } else {
      println("Could not initialize!")
      1
    }
  }

  final def initializeCommands() {
    val customCommands = commands
    rootSet.commands ++= customCommands
    validateCommands()
  }

  def validateCommands() = {
    rootSet.validateCommands()
  }

  private def interactiveMainLoop() {
    println("  Enter your commands below. Type 'help' for help. ")

    reader.setAutoprintThreshold(128) // allow more completions candidates without prompting
    reader.addCompleter(rootSet.argCompleter)

    var keepRunning = true
    while (keepRunning) {
      val line = reader.readLine(prompt)

      // Line is null on CTRL-D...
      if (line == null) {
        println()
        keepRunning = false
      } else {
        runKillableCommand(line)
      }
    }

    println("Exiting...")
  }

  private def runKillableCommand(line: String): Boolean = {
    val commandRunner = new KillableSingleThread(runCommand(line))

    // Wait a bit for completion, so quick commands don't need to go through the keyMonitor.
    val startTime = now
    commandRunner.start()
    commandRunner.waitForCompletion(Duration(200, TimeUnit.MILLISECONDS))

    // Start the keyMonitor to watch for key interrupts.
    if (!commandRunner.future.isCompleted) {
      interruptKeyMonitor.startMonitoring(interruptCallback = commandRunner.synchronized {
        if (!commandRunner.future.isCompleted) {
          println(s"Caught interrupt.")
          println(s"Killing command with 1s grace period: `$line`...")
          commandRunner.kill(Duration(1, TimeUnit.SECONDS))
        }
      })

      commandRunner.waitForCompletion(Duration.Inf)
      val runTimeInSec = (now - startTime) / 1000
      val timing = TimeFormats.formatAsTersePeriod(now - startTime)
      _logger.debug(s"Done running command `$line` (took: ${runTimeInSec}s [$timing]).")

      interruptKeyMonitor.stopMonitoring()
    }

    commandRunner.future.value match {
      case Some(Success(result)) => result
      case _ => false
    }
  }

  def runCommand(line: String): Boolean = {
    val tokens = parseLine(line)
    val tokensIterator = tokens.iterator
    while (tokensIterator.hasNext) {
      val newTokens = tokensIterator.takeWhile(_.trim != "&&").toList
      _logger.debug(s"Executing: $newTokens")
      if (verboseMode) {
        println(s"Executing: $newTokens")
      }
      val exitStatus = runSingleTokenizedCommand(newTokens)
      if (!exitStatus) {
        if (tokensIterator.hasNext) {
          println(s"Execution failed for `${newTokens.mkString(" ")}`.")
          println(s"Skipping the remaining commands: `${tokensIterator.toList.mkString(" ")}`")
        }
        return false
      }
    }
    true
  }

  private def runSingleTokenizedCommand(tokens: List[String]): Boolean = {
    val out = rootSet.executeLine(tokens)
    val msg = if (out) {
      s"[$name] Command finished successfully"
    } else {
      s"[$name] Command failed"
    }
    notificationManager.notify(msg)
    out
  }

  private def now = System.currentTimeMillis()

  protected def parseLine(line: String): List[String] = {

    if (line.trim.startsWith("#") || line.trim.length < 1) {
      return List[String]()
    }

    var parsedLine = List[String]()
    val regex = Pattern.compile("[^\\s\"'`]+|\"([^\"]*)\"|'([^']*)'|`([^`]*)`")
    val regexMatcher = regex.matcher(line)
    while (regexMatcher.find()) {
      if (regexMatcher.group(1) != null) {
        // Add double-quoted string without the quotes
        parsedLine :+= regexMatcher.group(1)
      } else if (regexMatcher.group(2) != null) {
        // Add single-quoted string without the quotes
        parsedLine :+= regexMatcher.group(2)
      } else if (regexMatcher.group(3) != null) {
        // Add `-quoted string with the quotes
        parsedLine :+= "`" + regexMatcher.group(3) + "`"
      } else {
        // Add unquoted word
        parsedLine :+= regexMatcher.group()
      }
    }

    parsedLine
  }

  // -----------------------------------------------------------------------------------------------
  // Little helpers.
  // -----------------------------------------------------------------------------------------------

  def getListParameter(cmdLine: CommandLine, name: String): Seq[String] = {
    val str = cmdLine.getOptionValue(name)
    if (str == null) {
      return List[String]()
    }

    str.split(",")
  }

  // -----------------------------------------------------------------------------------------------
  // Root commands.
  // -----------------------------------------------------------------------------------------------

  val subCommandExtractor = "`([^`]*)`".r // Regex extractor for commands within `-quotes.

  rootSet.commands += new ShellCommand("clear", "Clear the screen.") {
    def execute(cmdLine: CommandLine) = {
      new ConsoleReader().clearScreen
      true
    }
  }

  rootSet.commands += new ShellCommand("exit", "Quit the shell.", List("quit")) {
    def execute(cmdLine: CommandLine) = {
      System.exit(0)
      true
    }
  }

  rootSet.commands += new ShellCommand("sleep", "Sleeps the specified time period.", List("zzz")) {

    override def maxNumberOfArguments = 1

    def execute(cmdLine: CommandLine) = {
      val showSleepIndicator = cmdLine.checkFlag(ShowIndicatorFlag)

      cmdLine.get(PeriodArgument) match {
        case Some(period) =>
          val milliseconds = TimeFormats.parseTersePeriod(period).
            getOrElse(throw new IllegalArgumentException(s"Could not parse $period"))
          val canonicalPeriod = TimeFormats.formatWithMillis(milliseconds)
          println(s"Sleeping $canonicalPeriod.")

          val startTime = now
          var timeRemaining = milliseconds
          while (timeRemaining > 0) {
            try {
              Thread.sleep(1000L min timeRemaining)
            } catch {
              case ie: InterruptedException => _logger.debug("Error sleeping", ie)
            }
            if (showSleepIndicator) {
              print(".")
            }
            timeRemaining = startTime + milliseconds - now
          }
          if (showSleepIndicator) {
            println()
          }
          true

        case None =>
          println("Missing argument to sleep command!")
          false
      }
    }

    private val PeriodArgument = new CommandLineArgument("period", 0, true)
    private val ShowIndicatorFlag = new CommandLineFlag("v", "verbose", "whether to print . every second while sleeping")

    override def addOptions(opts: Options) {
      opts += PeriodArgument
      opts += ShowIndicatorFlag
    }
  }

  rootSet.commands += new ShellCommand("echo", "Write to the screen") {

    override def maxNumberOfArguments = -1 //unlimited

    def execute(cmdLine: CommandLine): Boolean = {
      if (cmdLine.getArgList.size() > 0) {
        println(cmdLine.getArgList.mkString(" "))
      }

      true
    }
  }

  rootSet.commands += new ShellCommand("tee", "Forks the stdout of a command so it also prints to a file") {

    private val CommandArgument = new CommandLineArgument("command", 0, true)
    private val OutputFileOption = new CommandLineOption("o", "outputFile", false, "Filename of output file (defaults to ~/tee.out)")
    private val AppendFileFlag = new CommandLineFlag("a", "append", "Append the output to the file rather than overwriting it")

    override def maxNumberOfArguments = 1

    override def addOptions(opts: Options) {
      opts += CommandArgument
      opts += OutputFileOption
      opts += AppendFileFlag
    }

    def execute(cmdLine: CommandLine) = {
      val outputFile = cmdLine.get(OutputFileOption).getOrElse(System.getProperty("user.home") + s"/tee.out")
      val appendFile = cmdLine.checkFlag(AppendFileFlag)

      cmdLine.get(CommandArgument) match {
        case Some(subCommandExtractor(cmd)) =>
          val fileOut = new FileOutputStream(outputFile, appendFile)
          val newOut = new PrintStream(new TeeOutputStream(Console.out, fileOut))
          val status = Console.withOut(newOut) {
            println(s"Running `$cmd` and outputting to '$outputFile' [append=$appendFile].")
            runCommand(cmd)
          }
          Try(fileOut.close())
          status
        case badCmd =>
          println(s"Usage: tee `<command>`, but found $badCmd.")
          false
      }
    }
  }

  rootSet.commands += new ShellCommand("time", "Measure the execution time of a command") {

    private val CommandArgument = new CommandLineArgument("command", 0, true)

    override def maxNumberOfArguments = 1

    override def addOptions(opts: Options) {
      opts += CommandArgument
    }

    def execute(cmdLine: CommandLine) = {
      cmdLine.get(CommandArgument) match {
        case Some(subCommandExtractor(cmd)) =>
          val start = now
          val exitStatus = runCommand(cmd)
          val dt = now - start
          val dtMessage = s"Execution took $dt ms (${TimeFormats.formatAsTersePeriod(dt)})"
          _logger.info(s"$dtMessage for `$cmd`")
          println(s"\n$dtMessage\n")
          exitStatus
        case badCmd =>
          println(s"Usage: time `<command>`, but found $badCmd.")
          false
      }
    }
  }

  rootSet.commands += new ShellCommand("run_script",
    "Run the script from the specified file.", List("script")) {

    override def maxNumberOfArguments = -1

    def execute(cmdLine: CommandLine): Boolean = {

      val continue = cmdLine.hasOption("continue")

      var scriptFile: File = null
      val args: Array[String] = cmdLine.getArgs

      if (args.length < 1) {
        printf("Please specify a script to run!")
        return false
      }

      val scriptFileName = args(0)

      for (pattern <- List("scripts/%s.dsh", "scripts/%s", "%s")) {
        val tmp = new File(pattern.format(scriptFileName))
        if (tmp.exists) {
          scriptFile = tmp
        }
      }

      if (scriptFile == null) {
        print(s"Could not find the script $scriptFileName! Please make sure the script file exists locally.")
        return false
      }

      // Execute the script, line by line.
      TimedBlock(s"Executing script $scriptFileName", println(_)) {
        val scriptLines = new ScriptRenderer(scriptFile, args.tail).
          getLines.filterNot(parseLine(_).isEmpty)
        require(scriptLines.nonEmpty, s"No non-comment lines found in $scriptFileName")
        for (line <- scriptLines) {
          val success = runCommand(line)
          if (!continue && !success) {
            return false
          }
        }
      }

      true
    }

    override def argCompleter: Completer = {
      if (scriptDir.exists) {
        var scriptNames = scriptDir.listFiles.filter(_.isFile).map(_.getName)
        if (scriptExtension != null) {
          scriptNames = scriptNames.filter(_.endsWith(scriptExtension))
        }
        new ArgumentCompleter(List(new StringsCompleter(scriptNames: _*), new NullCompleter))
      } else {
        new NullCompleter
      }
    }

    override def addOptions(opts: Options) = {
      opts.addOption("c", "continue", false, "Continue even if there was a failure in execution.")
    }
  }

  rootSet.commands += new NotificationCommandSet(notificationManager) // NOTE(chris, 2014-02-05): This has to be near the end for overrides to work
}
