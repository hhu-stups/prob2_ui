# Release History of ProB2-UI

Downloads of the current release and snapshot development builds can be found on [our main download page](https://prob.hhu.de/w/index.php?title=Download#ProB2-UI).

## Version 1.3.1 (not released yet)

* Added transition information stored in state space to operation details stage (e.g. details about the execution of operations with STORE_DETAILED_TRANSITION_INFOS preference)
* Added menu for data import of XML files with XML2B
* Improved RulesView (display messages for UNCHECKED rules)
* Improved XTL Prolog mode 
  * Enabled execute by predicate for registered transitions (`trans_prop`), can be used to specify parameter values
  * Enabled JSON trace replay, also with interactive trace replay
  * Enabled simulation with SimB (basic functionality, without caching)
  * Improved support for visualisation with VisB (using B definitions)

## Version 1.3.0 (2025-07-30)

* Updated the ProB core to version 1.15.0 - see the [ProB Release History](https://prob.hhu.de/w/index.php/ProB_Release_History)
* Added interactive trace replay for user-controlled replay of traces, useful for refinements or refactoring of traces
* Added options for deterministic animation steps, as long as possible 
* Added B type information for code completion (if available)
* Added VisB export as SVG in addition to PNG
* Added customization options for VisB HTML export
* Added TLC model checking for classical B machines with TLC4B and export of generated TLA+ files (also limited support for Event-B and models with internal B representation)
* Added menu item for exporting a B machine as a TLA+ module
* Improved support for rule validations with rules machines (.rmch)
* Added trace replay support for XTL Prolog models
* Added graphical visualization of SimB activation diagrams
* Added a default SimB configuration that can be used when no configuration has been written yet
* Added deterministic animation menu to operations and history views (under "fast forward" button)
* Added state limit option to LTL/CTL model checking
* Added option for LTL/CTL model checking starting from a specific state (instead of starting from all initial states)
* Added ability to copy validation tasks between machines in the same project
* Added ability to change a machine's location
* Added ability to duplicate machines
* Added tooltip for machine descriptions in project machine list
* Added preference for automatically reloading the current machine after saving in the editor
* Added search to editor
* Added line numbers and syntax highlighting to internal representation window
* Added syntax highlighting to ProB and Groovy consoles
* Added error highlighting to formula input text fields in visualization windows
* Added language selection dropdown to interactive console (e.g. evaluation of both classical B and TLA+ formulas in the context of a translated TLA+ module)
* Added state view context menu entries for quickly visualizing a value as a graph or table
* Added option to display the full text for large values in state view "Show details" window
* Added menu option for editing the current VisB file in external editor
* Added context menu option for copying error messages in error alerts
* Added keyboard support to history view
* Added support for moving backwards/forwards through history using mouse back/forward buttons (mouse buttons 4 and 5)
* Added zoom gesture support to VisB (for multi-touch trackpads and tablet touchscreens)
* Operations view shows more clearly if a timeout occurred while calculating all outgoing transitions
* Simplified executing operations that timed out or that have `MAX_OPERATIONS` set to 0
* Improved formatting feature in state view "Show details" window
* Improved export of (internal) classical B representation for Event-B and translated languages
* When deleting a trace from a project, the user is now asked whether the trace file should also be deleted from disk
* If model checking stops before fully finishing, the user can choose whether to resume model checking from this point or to restart from the beginning
* Improved error display when editing LTL/CTL formulas and LTL patterns
* Combined graph and table visualization into a single window
* Simplified UI for changing the default VisB visualization
* Improved feedback in VisB view if the model is not initialized yet (the visualization is no longer displayed in an uninitialized state)
* Improved layout of SimB window
* Improved SimB JSON format:
  * SimB JSON configurations can now be edited and saved in the SimB window, not just by editing the JSON file directly
  * `probabilisticVariables` is now split up into `probabilisticVariables` and `transitionSelection` (refer to https://prob.hhu.de/w/index.php?title=SimB#Direct_Activation for more information)
  * All probabilities are interpreted as weights: they do not have to sum to one anymore
* Improved SimB Monte Carlo simulation:
  * SimB validation tasks can now be edited after creation
  * Added more estimation types (minimum/maximum, mean between steps)
  * Time can now be any B expression, not just a constant value
  * Progress information is shown while a Monte Carlo simulation is running
* Changed SimB external simulator (RL agent) protocol:
  * The protocol now uses JSON instead of simple lines of text
  * The external simulator is sent a list of all enabled operations from which it may choose (rather than the external simulator choosing an operation and then SimB replying whether that operation is allowed or not)
  * Removed support for `SHIELD_INTERVENTION` definitions
* Improved layout of test case generation details window
* Improved layout of toolbars with many buttons
* Improved VO manager (still experimental):
  * Added ability to check all VOs in a machine or an entire project
  * Symbolic checking, symbolic animation, test case generation, and history chart formulas can now be used as validation tasks
  * Validation task statuses are now preserved when switching between machines (as long as the machine to which the tasks belong hasn't been modified)
  * Validation tasks are not re-executed if they have already been executed before and their result is already known
  * Improved error feedback when creating requirements
  * Fixed VO status icons sometimes incorrectly turning yellow
  * Removed formal distinction between requirement types (functional and non-functional) as the VO manager doesn't use this information
* Improved UI responsiveness while updating the status bar or a VisB visualization
* Improved startup performance slightly
* Fixed multiple problems when starting multiple instances of ProB2-UI at once
* Fixed editor discarding changes made while saving
* Fixed editor sometimes overwriting file contents when using undo together with switching between files
* Fixed various text editing bugs in ProB and Groovy consoles
* Fixed number input bugs when configuring MC/DC test case generation
* Fixed model checking result "Show Trace" button sometimes showing the wrong trace (if multiple errors were found) or unloading the machine
* Fixed proof obligation view working only for machines and not for contexts
* Fixed history chart formulas sometimes disappearing
* Fixed statistics possibly not updating after long-running tasks
* Fixed preference windows not showing the correct "changed" status of preferences
* Fixed various bugs in SimB
* Fixed various bugs in documentation generator
* Fixed inability to cancel certain tasks, such as random animation
* Fixed certain internal errors not being displayed to user
* Fixed internal error when interrupting/cancelling certain tasks, such as loading a machine or model checking
* Fixed text rendering problems in help window on some systems
* Fixed rare random exception on startup
* Fixed reattaching of detached views sometimes not working properly
* Fixed the Quit menu item on macOS causing open windows to not be restored on next startup
* Fixed windows visibly jumping around when opened
* Removed default description from projects automatically created from files
* Removed save/load feature for LTL formulas and patterns (replaced by new feature for copying validation tasks inside a project)
* Removed support for Java versions older than Java 21
* Removed support for Windows versions older than Windows 10 and macOS versions older than macOS 11

**Note:** The multi-platform jar might not work on all macOS processor architectures - consider using the platform-specific binaries, which are available for both x86_64 and arm64.

### Downloads for version 1.3.0

* [Standalone jar file (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.3.0/prob2-ui-1.3.0-multi.jar)
* [Windows installer (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.3.0/ProB%202%20UI-1.3.0.exe)
* [macOS arm64 (Apple Silicon) application (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.3.0/ProB%202%20UI-aarch64-1.3.0.dmg)
* [macOS x86_64 (Intel) application (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.3.0/ProB%202%20UI-x86_64-1.3.0.dmg)
* [Linux deb package (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.3.0/prob2-ui_1.3.0_amd64.deb)

## Version 1.2.1 (2023-08-15)

* Updated the ProB core to version 1.12.2 - see the [ProB Release History](https://prob.hhu.de/w/index.php/ProB_Release_History)
* Added binaries for macOS on arm64 (Apple Silicon) in addition to x86\_64 (Intel)
* SimB simulation can be controlled by an external simulator, e. g. a reinforcement learning agent
* Decisions made by reinforcement learning agent can be corrected by a safety shield via a `SHIELD_INTERVENTION` definition
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

**Note:** This is the last release to support Java 8, 11, and 17. The next release will require Java 21 or newer. This only affects users of the multi-platform jar - consider using the platform-specific binaries, which have an appropriate JRE version bundled.

**Note:** This is the last release to support Windows 8.1 and older and macOS 10.15 and older. The next release will require at least Windows 10 or macOS 11.

**Note:** The multi-platform jar might not work on all macOS processor architectures - consider using the platform-specific binaries, which are available for both x86_64 and arm64.

### Downloads for version 1.2.1

* [Standalone jar file (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.2.1/prob2-ui-1.2.1-multi.jar)
* [Windows installer (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.2.1/ProB%202%20UI-1.2.1.exe)
* [macOS arm64 (Apple Silicon) application (with bundled JRE, not signed or notarized)](https://stups.hhu-hosting.de/downloads/prob2/1.2.1/ProB%202%20UI-aarch64-1.2.1.dmg)
* [macOS x86_64 (Intel) application (with bundled JRE, not signed or notarized)](https://stups.hhu-hosting.de/downloads/prob2/1.2.1/ProB%202%20UI-x86_64-1.2.1.dmg)
* [Linux deb package (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.2.1/prob2-ui_1.2.1-1_amd64.deb)

## Version 1.2.0 (2023-06-22)

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
* Fixed errors with the config file when switching between different versions of ProB2-UI

### Downloads for version 1.2.0

* [Standalone jar file (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.2.0/prob2-ui-1.2.0-multi.jar)
* [Windows installer (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.2.0/ProB%202%20UI-1.2.0.exe)
* [macOS x86_64 (Intel) application (with bundled JRE, not signed/notarized)](https://stups.hhu-hosting.de/downloads/prob2/1.2.0/ProB%202%20UI-1.2.0.dmg)
* [Linux deb package (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.2.0/prob2-ui_1.2.0-1_amd64.deb)

## Version 1.1.0 (2021-01-26)

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
* [macOS x86_64 (Intel) application (with bundled JRE, notarized)](https://stups.hhu-hosting.de/downloads/prob2/1.1.0/ProB2-UI-1.1.0-notarized.zip)
* [Linux deb package (with bundled JRE)](https://stups.hhu-hosting.de/downloads/prob2/1.1.0/prob2-ui_1.1.0-1_amd64.deb)

## Version 1.0.0 (2019-07-18)

* First stable release.

### Downloads for version 1.0.0

* [Standalone jar file (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.0.0/prob2-ui-1.0.0-all.jar)
* [Zip distribution (all platforms)](https://stups.hhu-hosting.de/downloads/prob2/1.0.0/prob2-ui-1.0.0.zip)
* [macOS x86_64 (Intel) application (without JRE, not signed or notarized)](https://stups.hhu-hosting.de/downloads/prob2/1.0.0/prob2-ui-1.0.0-mac.zip)
