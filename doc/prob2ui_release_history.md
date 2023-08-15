# Release History of ProB2-UI

Downloads of the current release and snapshot development builds can be found on [our main download page](https://prob.hhu.de/w/index.php?title=Download#ProB2-UI).

## Version 1.2.1 (not released yet)

* Updated the ProB core to version 1.12.2 - see the [ProB Release History](https://prob.hhu.de/w/index.php/ProB_Release_History)
* Added binaries for macOS on arm64 (Apple Silicon) in addition to x86\_64 (Intel)
* SimB simulation can be controlled by an external simulator, e. g. a reinforcement learning agent
* Decisions made by reinforcement learning agent can be corrected by a safety shield via a `SHIELD_INTERVENTION`definition
* Added estimation of average and cumulative sum to SimB
* Added average trace length to SimB statistics
* Added CSV export of SimB statistics with trace length and estimated value
* Added column for number of steps to trace replay table
* Added syntax highlighting for a few more code text fields
* Editor now asks for confirmation before discarding unsaved changes when reloading the machine
* Allowed resetting a manually changed VisB visualization path back to the `VISB_JSON_FILE` default
* Fixed editor syntax highlighting sometimes not working on Windows
* Improved error messages when a machine file could not be read
* Improved handling of invalid values for ProB preferences
* Improved feedback for internal errors in VisB

**Note:** This is the last release to support Java 8 and 11. The next release will require Java 17 or newer. This only affects users of the multi-platform jar - consider using the platform-specific binaries, which have an appropriate JRE version bundled.

## Version 1.2.0 (22nd of June 2023)

* Updated the ProB core to version 1.12.1 - see the [ProB Release History](https://prob.hhu.de/w/index.php/ProB_Release_History)
* Added [SimB](https://prob.hhu.de/w/index.php?title=SimB), a simulator built on top of ProB, supporting interactive simulation and validation of probabilistic and timing properties
* Added prototype version of VO manager for creating and checking validation obligations, developed as part of the [IVOIRE project](https://isse.jku.at/ivoire/)
* Added automatic documentation generator that creates a LaTeX/PDF report of a project's models and verification statuses
* Added support for CTL model checking in addition to LTL
* Added code completion support in machine editor, B console, and B formulas in LTL/CTL
* Reworked trace replay to be more flexible regarding model changes and give better feedback about errors and incomplete replay 
* Trace files support postcondition tests and descriptions for each step
* Added advanced trace refactoring for adapting traces to machine changes or refinements
* Integrated VisB into main window (the visualization pane can be detached into a standalone window if desired)
* VisB visualizations can be exported as static HTML files
* History of transitions can be exported as CSV
* State view can be sorted alphabetically
* Model checking can use a different goal predicate than the machine's `GOAL` definition
* Test case generation supports Event-B models
* Verifications view displays status of proof obligations in Rodin projects
* Graph, table, and history chart visualization formulas are saved in the project file
* History chart curves can be made rectangular instead of interpolated
* Rodin projects can be exported as .eventb files (compatible with probcli and ProB Tcl/Tk)
* Simplified UI layout in various places
* Improved text editor syntax highlighting (Event-B support, more keywords)
* Improved error handling when loading missing or invalid files
* Improved handling of errors during startup
* ProB core process can be manually interrupted and restarted via ProB Core Console (in case of hangs and internal errors)
* Fixed UI responsiveness when executing operations and during model checking
* Fixed inefficient expansion of symbolic sets in some places
* Fixed various keyboard problems with some OSes and keyboard layouts (Shift on Windows, undo/redo with non-US layouts, zoom shortcuts, consoles)
* Fixed some toolbar icons becoming invisible for small window/view sizes
* Fixed errors with the config file when switching between different versions of ProB 2 UI

### Downloads for version 1.2.0

* [Standalone jar file (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.2.0/prob2-ui-1.2.0-multi.jar)
* [Windows installer (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.2.0/ProB%202%20UI-1.2.0.exe)
* [macOS application (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.2.0/ProB%202%20UI-1.2.0.dmg)
* [Linux deb package (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.2.0/prob2-ui_1.2.0-1_amd64.deb)

## Version 1.1.0 (26th of January 2021)

* now contains [VisB](https://prob.hhu.de/w/index.php?title=VisB) directly in the application, VisB has been extended considerably now supporting hovers, more attributes, better debugging and error feedback, ...
* supports model checking with time and state limit
* improved feedback for model checking (progress bar, memory usage)
* improved feedback for trace replay
* supports well-definedness checking (as an option under symbolic model checking)
* improved test case generation view with ability to save generated traces
* supports static analysis on machine
* supports exporting graphs from graph visualization view as .dot, .png, and .pdf
* added table visualization options for jumping to state IDs and source code locations
* improved state view that supports expanding formulas
* improved error feedback in the animator, especially when the machine could not be initialized
* improved highlighting of errors in editor
* added option to control warning detail level - can disable warnings or enable additional messages
* contains Prolog Output Console for debugging
* supports syntax highlighting for TLA, CSP, Alloy, XTL, and Z
* fixed Z support on macOS and Linux
* improved performance in various places, especially on startup, when switching machines, and when large machines are loaded

### Downloads for version 1.1.0

* [Standalone jar file (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.1.0/prob2-ui-1.1.0-multi.jar)
* [Windows installer (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.1.0/ProB2UI-1.1.0.exe)
* [macOS application (with bundled JRE, notarized)](https://stups.hhu-hosting.de/downloads/prob2/1.1.0/ProB2-UI-1.1.0-notarized.zip)
* [Linux deb package (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.1.0/prob2-ui_1.1.0-1_amd64.deb)

## Version 1.0.0 (18th of July 2019)

* First stable release.

### Downloads for version 1.0.0

* [Standalone jar file (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.0.0/prob2-ui-1.0.0-all.jar)
* [Zip distribution (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.0.0/prob2-ui-1.0.0.zip)
* [macOS application (without JRE, not notarized)](https://stups.hhu-hosting.de/downloads/prob2/1.0.0/prob2-ui-1.0.0-mac.zip)
