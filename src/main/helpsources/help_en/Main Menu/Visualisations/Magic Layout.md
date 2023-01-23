![Magic Layout](../../../screenshots/Visualisations/MagicLayout.png)

Magic Layout View provides the possibility for interactive and customizable state visualization.
The view is divided into the [Settings View](#settings) on the left side and the [Graph View](#graph) on the right side.

## <a name="settings"></a>Settings View
Settings View is divided into two tabs, one for node groups and one for edge groups. Both however are identical with regard to their functionality.

You can add new groups via file/new/... or the context menu of the listview .

Double-clicking on a group in the list or using the context menu allows to rename an existing group. You can delete existing groups using the context menu of the specific group. 

By default, node and edge groups representing the sets, constants and variables of the current machine are added when opening the Magic Layout View. 

Each of this predefined groups is assigned a random font color, which can be customized in the dropdown-menus in the bottom left corner. To apply the settings, the visualisation
must be reloaded by clicking on the "Update"-button in the graph view.

The text field below the list view allows to specify, which nodes or edges are part of the currently selected node or edge group by entering a valid B expression.

The settings are applied to the graph in the order they appear in the list view. The further down a node or edge group is listed, the later the specified settings are applied.
New node and edge groups are added at the bottom of the list. The order of the groups in the list can be changed via drag-and-drop.

In the file-menu you can save and load layout settings and save the graph as image.


## <a name="graph"></a>Graph View

The graph-view shows the visualisation of the state. 

The displayed portion of the graph can be adjusted by scrolling and zooming. In general, you would click the plus and the minus button to zoom in and out. 
If you use a Touch-Enabled Device, you could also zoom with a two-fingered zoom gesture.

Via the "update"-button, the chosen layout settings will be applied to the state graph. 

The Layout Button will furthermore relocate all nodes regarding the selected layout algorithm. Currently, there ape two layout options:
* layered: the nodes are placed in different layers below each other
* random: the postions of the nodes is random. 

Also, the nodes can be moved via drag-and-drop. This allows for a custom layout of the graph.
