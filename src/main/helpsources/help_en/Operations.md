![Operationen](../screenshots/Operations.png)

The Operations View shows the operations on a machine, if a machine is loaded.
If you click on an operation shown in the list, it will be executed if possible.
The icons and marks next to the operations show, if an operations is executable and which state will be reached.
* <span style="color:green">green</span> triangle: operations is executable
* <span style="color:red">red</span> circle: operation is not executable
* <span style="color:#1284F7">blue</span> circular arrow: operation is executable but leads to the current state  

* <span style="color:#037875">darkgreen</span> mark: operation leads to unexplored state (which might be an errored state)
* <span style="color:#B77300">orange</span> mark: operation leads errored state  


The icons in the upper right corner of the view allow you to
* sort operations in alphabetical order
* hide non-executable or inactive operations
* show/hide unambiguous operations variables
* execute a specific number of randomized operations
* filter operations

Using the arrow in the upper left corner, you can jump back and forth in history or reload the machine.
By right-clicking on an operation, you can inspect details about it or add a formula, that should be considered when executing the operation.

