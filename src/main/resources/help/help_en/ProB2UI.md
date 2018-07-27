# The ProB2 JavaFX Main Window

By default the main window is split into three vertical panes (see below).

* In the left pane, the [Operations view](Operations.md) , showing the operations whose preconditions and guards are true in this state (the view also uses a blue circular arrow icon when an operation does not change the state);
* In the middle 
	* the [State View](Main%20View/State.md), containing the current state of the B machine, listing e.g., the current values of the machine variables,
	* the [State Errors View](Main%20View/State%20Errors.md), containing possible state errors and
	* the [Visualisation View](Main%20View/Visualisation.md), containing a visualisation, if provided by the user;
* In the right pane there are a variety of subviews, which can be activated:
	* [The History of operations leading to this state (History)](History.md)
	* [The Project view](Project.md)
	* [The Verification view](Verification.md)
	* [The Statistics view](Statistics.md)

![ProB2 JavaFX UI Overview](../screenshots/Overview.png)

[More on ProB](https://www3.hhu.de/stups/prob/index.php/Main_Page)

# The ProB2 JavaFX Main Menu Bar

The menu bar contains the various commands to access the features of ProB. It includes the menus
* File,
* View,
* Visualisation,
* Advanced,
* Window and
* Help

![File Menu](../screenshots/Menu/File.png)

The File submenu allows you to create a new Project, open an existing project or a machine, open recent projects shown as list and/or clear the list of recent projects, close the ProB2 JavaFX UI, save your project, reload the currently running machine or edit preferences.

![View Menu](../screenshots/Menu/View.png)

This submenu allows you to adjust font and button size in the ProB2 JavaFX UI or enter full screen mode.

![Visualisation Menu](../screenshots/Menu/Visualisation.png)

The Visualisation submenu provides different forms of visualisation. You can enter a formula, view the history chart or take a look at the graph visualisation.

![Advanced Menu](../screenshots/Menu/Advanced.png)

This submenu provides a Groovy console and allows you to manage your plugins and own visualisations.

![Window Menu](../screenshots/Menu/Window.png)

The Window submenu allows you to change perspectives and detach components. This UI comes with 3 different preset perspectives. It allows you to use your own perspective as well by providing a FXML file containing the views but be aware that this might ruin the ability to detach components. [How to create FXML files](Perspectives.md)

![Help Menu](../screenshots/Menu/Help.png)

The Help submenu provides you with help about the ProB2 JavaFX UI, information about the ProB2 UI, ProB2 kernel, ProB CLI and Java version used here and a way to report issues regarding the ProB2 JavaFX UI.
 
