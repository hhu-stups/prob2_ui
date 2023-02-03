# The ProB2 JavaFX Main Window

By default, the main window is split into three vertical panes:

* In the left pane
  *	 the [Operations View](Operations.md), showing the operations whose preconditions and guards are true in this state,
  *  the [Animations View](Animation.md), providing different kinds of animation for the current machine.


* In the middle 
	* the [State View](Main%20View/State%20View.md), containing the current state of the B machine, 
    * the [State Visualisation](Main%20View/State%20Visualisation.md), containing a visualisation, 
    * the [Editor](Main%20View/Editor.md), to edit und save currently loaded machines,
	* the [B Console](Main%20View/B%20Console.md), a built-in console to evaluate B-formulas.
  

* In the right pane there's a variety of subviews, which can be activated:
	* the [History](History.md) of operations leading to the current state,
	* the [Project View](Project.md), showing the machines belonging to the project, the verification status, preferences and an overview of the project,
	* the [Verifications View](Verification.md), offering methods to test a machine,
	* the [Statistics](Statistics.md) of the current project, providing data about the state and transitions.


In the bottom left corner, the [State Errors](Main%20View/State%20Errors.md), such as unsatisfied invariants or deadlocks, are displayed.

The [Main Menu Bar](Main%20Menu/Main%20Menu%20Bar.md) offers further options, including possibilities to visualize machines, manage your projects and customize the perspective  


![ProB2 JavaFX UI Overview](../screenshots/Overview.png)

[More on ProB](https://prob.hhu.de/w/)
