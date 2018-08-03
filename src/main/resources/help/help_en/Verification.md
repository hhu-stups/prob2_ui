# Verification View

The Verification View provides 4 different methods to test a machine:

* Modelchecking,
* LTL Verifications,
* Symbolic Checking and
* Trace Replay

In each tab you can add multiple tests to check you currently selected machine and interrupt the checking process by pressing the "Cancel" button.

## <a id="Model"> Modelchecking </a>

![Modelchecking](../screenshots/Verifications/Modelchecking.png)

By pressing the plus button you can add several model checking variants. The following view will be shown:

![Modelchecking Stage](../screenshots/Verifications/Modelchecking%20Stage.png)

Select one of the search strategies (breadth first, depth first or a mix of both) and the checkboxes containing  different possible errors like deadlocks to be checked for. By pushing the "Model Check" button your selected variant will be added to the list shown at the top of the Modelchecking Tab.

## <a id="LTL"> LTL Verifications </a>

![LTL](../screenshots/Verifications/LTL.png)

By pressing the "Add LTL Formula" or "Add LTL Pattern" buttons an editor for each respectively will be opened and you can add LTL formulas or patterns to the lists to be checked for.

## <a id="Symbolic"> Symbolic Checking </a>

![Symbolic Checking](../screenshots/Verifications/Symbolic%20Checking.png)

By pressing the plus button you can add several symbolic checking variants. The following view will be shown:

![Add SC](../screenshots/Verifications/Add%20SC.png)

The dropdown menu allows you to select the type of check. Some variants of the symbolic checking might need additional parameters (e.g. Invariant needs an operation).

## <a id="Trace"> Trace Replay </a>

![Trace Replay](../screenshots/Verifications/Trace%20Replay.png)

The Trace Replay tab allows you to load a trace from your hard drive to be replayed by the machine.
