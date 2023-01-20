# <a name="top"></a>
# The ProB2 JavaFX Main Window

By default, the main window is split into three vertical panes (see below).

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

It is possible to [detach](Detaching%20of%20Components.md) the single components und put them in separate windows or customize the appearance of the UI by using the [Perspective](Perspectives.md)-option.

![ProB2 JavaFX UI Overview](../screenshots/Overview.png)

[More on ProB](https://www3.hhu.de/stups/prob/index.php/Main_Page)

# The ProB2 JavaFX Main Menu Bar

The menu bar contains the various commands to access the features of ProB. It includes the menus
* [File](#File),
* [View](#View),
* [Visualisation](#Visualisation),
* [Advanced](#Advanced),
* [Window](#Window) and
* [Help](#Help)

## <a name="File"> File</a>
![File Menu](../screenshots/Menu/File.png)

The File submenu allows you to create a new [Project](Project.md), open an existing project or open or clear the list of recent projects.

You can save your project or machine, reload it, run an extended statistics analysis or take a look at the current machine in a formatted manner. 

Furthermore, it is possible to export the model in different types. 

In the menu "Preferences" you can customize the settings of the UI and the current project itself. The "Close Window" allows you to close the ProB 2 UI.

## <a name="View"> View</a>
![View Menu](../screenshots/Menu/View.png)

This submenu allows you to adjust and reset the size of the layout in the ProB2 JavaFX UI or enter full screen mode.

## <a name="Visualisation"> Visualisation</a>
![Visualisation Menu](../screenshots/Menu/Visualisation.png)

The Visualisation submenu provides different forms of visualisation. 

You can visualize the transition and states of the machine as a [graph](Main%20View/Formula%20Graph%20Visualisation.md),
a [table](Main%20View/Formula%20Table%20Visualisation.md) or by using the [Magic Layout](Magic%20Layout.md), a tool for interactive and customizable state visualisation.

The "Open VisB" option offers a visualisation of the machine, if corresponding .json and .svg-files are provided.

Furthermore, the [history chart](History%20Chart.md) shows a chart of the chosen formulas.

## <a name="Advanced"> Advanced</a>
![Advanced Menu](../screenshots/Menu/Advanced.png)

This submenu provides a Groovy and a ProB core console and allows you to manage your plugins and own visualisations. 

The [SimB]-option provides simulations for formal models.

## <a name="Window"> Window</a>
![Window Menu](../screenshots/Menu/Window.png)

The Window submenu allows you to change perspectives and [detach components](Detaching%20of%20Components.md) as described above. 

This UI comes with 3 different preset perspectives. It allows you to [use your own perspective](Perspectives.md) as well by providing a FXML file containing the views but be aware that this might ruin the ability to detach components.


## <a name="Help"> Help</a>
![Help Menu](../screenshots/Menu/Help.png)

The Help submenu provides you with help about the ProB2 JavaFX UI, information about the ProB2 UI (containing details about the ProB2 kernel, the ProB CLI and the Java version used),
a way to report issues regarding the ProB2 JavaFX UI and an overview about the syntax of specific languages, e.g. B, TLA or CSP.

[back to top](#top)
