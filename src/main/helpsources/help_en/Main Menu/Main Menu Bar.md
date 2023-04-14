# <a name="top"></a>

The menu bar contains the various commands to access the features of ProB. It includes the menus
* [File](#File),
* [View](#View),
* [Visualisation](#Visualisation),
* [Advanced](#Advanced),
* [Window](#Window) and
* [Help](#Help)

## <a name="File"> File</a>
![File Menu](../../screenshots/Menu/File.png)

The File submenu allows you to create a new [Project](../Project.md), open an existing project or open or clear the list of recent projects.

You can save your project or machine, reload it, run an extended statistics analysis or take a look at the internal representation of the current machine.
Furthermore, it is possible to export the model in different types.

In the menu "Preferences" you can customize the settings of the UI and the current project itself. The "Close Window" allows you to close the ProB 2 UI.

## <a name="View"> View</a>
![View Menu](../../screenshots/Menu/View.png)

This submenu allows you to adjust and reset the size of the layout in the ProB2 JavaFX UI or enter full screen mode.

## <a name="Visualisation"> Visualisation</a>
![Visualisation Menu](../../screenshots/Menu/Visualisation.png)

The Visualisation submenu provides different forms of visualisation.

You can visualize the transition and states of the machine as a [graph](Visualisations/Graph%20Visualisation.md),
a [table](Visualisations/Table%20Visualisation.md) or by using the [Magic Layout](Visualisations/Magic%20Layout.md), a tool for interactive and customizable state visualisation.

Furthermore, the [history chart](Visualisations/History%20Chart.md) shows a chart of the chosen formulas.

## <a name="Advanced"> Advanced</a>
![Advanced Menu](../../screenshots/Menu/Advanced.png)

This submenu provides a Groovy and a ProB core console and allows you to manage your plugins and own visualisations.

The [SimB]-option provides automatic simulations for formal models.




## <a name="Window"> Window</a>
![Window Menu](../../screenshots/Menu/Window.png)

The Window submenu allows you to change perspectives and [detach components](Window/Detaching%20of%20Components.md) as described above.

This UI comes with 3 different preset perspectives. 
It allows you to [use your own perspective](Window/Perspectives.md) as well by providing a FXML file containing the views 
but be aware that this might ruin the ability to detach components.



## <a name="Help"> Help</a>
![Help Menu](../../screenshots/Menu/Help.png)

The Help submenu provides you with help about the ProB2 JavaFX UI, information about the ProB2 UI (containing details about the ProB2 kernel, the ProB CLI and the Java version used),
a way to report issues regarding the ProB2 JavaFX UI and an overview about the syntax of specific languages, e.g. B, TLA or CSP.

[back to top](#top)
