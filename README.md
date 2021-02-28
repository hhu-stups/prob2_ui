# ProB 2 Standalone GUI

## Requirements

The ProB 2 UI requires Java 8 or newer, and has been tested with Oracle JDK 8, 9, and 10.

## Download

Pre-built binaries for the ProB 2 UI can be downloaded [here](https://www3.hhu.de/stups/downloads/prob2/).

## Running from source

The ProB 2 UI can be started from source using the Gradle `run` task (`./gradlew run` on Linux/macOS/etc., `gradlew.bat run` on Windows).

Building the ProB 2 UI from source requires [Pandoc](https://pandoc.org/) to generate the help files for the UI's built-in help function. Pandoc is available from all popular package managers, or as an installer from [the Pandoc website](https://pandoc.org/installing.html).

If you want to build the UI without installing Pandoc, you can pass the option `--exclude-task createHelp` to Gradle to skip the help build. The UI is fully functional without the built help files, except that the help window will have no content.

## Main Features ##

![Main Window of ProB2-UI](/src/doc/prob2ui-screenshot2.png?raw=true "Main Window of ProB2-UI")
