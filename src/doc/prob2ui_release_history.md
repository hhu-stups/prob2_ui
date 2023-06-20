# Release History of ProB2-UI

Downloads of the current release and snapshot development builds can be found on [our main download page](https://prob.hhu.de/w/index.php?title=Download#ProB2-UI).

## Version 1.2.0 (not released yet)

* [SimB](https://prob.hhu.de/w/index.php?title=SimB)

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
