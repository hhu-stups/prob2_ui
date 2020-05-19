# Magic Layout

<img src="../screenshots/MagicLayout.png" alt="Magic Layout View"
	title="Magic Layout View Screenshot" width="750" />

Magic Layout View provides the possibility for interactive and customizable state visualization.
The view is divided into the [Graph View](#graph) on the right side and the [Settings View](#settings) on the left side.

## <a name="graph"></a>Graph View

The displayed portion of the graph can be adjusted by scrolling and zooming. In general you would click the plus and the minus button to zoom in and out. If you use a Touch-Enabled Device, you could also zoom with a two-fingered zoom gesture.
If you click on the Update Button, the chosen layout settings will be applied to the state graph. The Layout Button will furthermore relocate all nodes regarding the selected layout algorithm. Currently a layered layout which places the nodes in different layers below each other and a random layout which places all nodes at random coordinates are implemented. Also the nodes can be moved via drag-and-drop. This allows for a custom layout of the graph.

## <a name="settings"></a>Settings View
Settings View is divided into two Tabs, one for node groups and one for edge groups. Both however are identical with regard to their functionality.

New node and edge groups can be added by selecting File/New/Node Group or File/New/Edge Group in the menu bar or via the context menu of the list view. Double clicking on a group in the list allows to rename an existing group. You can also rename and delete existing groups using the context menu of the specific group. It is not possible for two groups to have the same name. By default node and edge groups representing the sets, constants and variables of the current machine are added when opening the Magic Layout View. Each of this predefined groups is assigned a random initial color.

The text field below the list view allows to specify which nodes or edges are part of the currently selected node or edge group by entering a valid B expression. 

The selection buttons below the text field allow to specify the desired settings for the currently selected node or edge group. To apply the settings to the graph use the appropriate buttons in the [Graph View](#graph) to update or layout the graph.

The settings are applied to the graph in the order they appear in the list view. The further down a node or edge group is listed, the later the specified settings are applied. New node and edge groups are added at the bottom of the list. The order of the groups in the list can be changed via drag-and-drop.

File/Save Layout Settings... allows to save and File/Load Layout Settings... allows to load defined settings.

You can save the graph as image by clicking on File/Save Graph as Image in the menu bar.