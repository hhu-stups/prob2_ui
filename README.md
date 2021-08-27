# ProB 2 Standalone GUI

ProB2-UI is a modern JavaFX-based user interface for the animator, constraint solver, and model checker [ProB](https://prob.hhu.de).
In addition to the [B-method](http://en.wikipedia.org/wiki/B-Method), ProB also supports [Event-B](http://www.event-b.org), [CSP-M](http://en.wikipedia.org/wiki/Communicating_sequential_processes), [TLA+](http://research.microsoft.com/en-us/um/people/lamport/tla/tla.html), and [Z](http://en.wikipedia.org/wiki/Z_notation).

Paper: https://www.researchgate.net/publication/353989368_ProB2-UI_A_Java-Based_User_Interface_for_ProB

Citation:

```
@InProceedings{prob2ui,
author="Bendisposto, Jens
and Gele{\ss}us, David
and Jansing, Yumiko
and Leuschel, Michael
and P{\"u}tz, Antonia
and Vu, Fabian
and Werth, Michelle",
editor="Lluch Lafuente, Alberto
and Mavridou, Anastasia",
title="ProB2-UI: A Java-Based User Interface for ProB",
booktitle="Formal Methods for Industrial Critical Systems",
year="2021",
publisher="Springer International Publishing",
address="Cham",
pages="193--201",
abstract="ProB2-UI is a modern JavaFX-based user interface for the animator, constraint solver, and model checker ProB. We present the main features of the tool, especially compared to ProB's previous user interfaces and other available tools for B, Event-B, and other formalisms. We also present some of ProB2-UI's history as well as its uses in the industry since its release in 2019.",
isbn="978-3-030-85248-1"
}
```


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
