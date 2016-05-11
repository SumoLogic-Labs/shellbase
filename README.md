# shellbase
Sumo Logic's Scala-based interactive shell framework

# Built-In Commands

* **help** / **?** - Displays a summary of available commands.
* **clear** - Clears the text on the screen
* **exit** / **quit** - Exits the current shell
* **sleep** / **zzz** - Sleeps for the specified duration.  Can use compact time: `5m` means `5 minutes`
* **echo** - Writes output to the screen (STDOUT)
* **tee** - Lets you fork stdout to a file
* **time** - Time how long a command took
* **run_script** / **script** - Execute the specified script.  Will attempt to be smart about locating 

# Example Shell

We've put together a very simple [example shell](./shellbase-example) that can generate random numbers.
