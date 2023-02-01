# <a name="top"></a>
The Verification View provides four different methods to test a machine:

* [Model Checking](#Model),
* [LTL Verifications](#LTL),
* [Symbolic Checking](#Symbolic) and
* [Proof Obligation](#ProofObligation)

In each tab you can add multiple tests to check you currently selected machine.

The process can be restarted or interrupted by clicking the x-button in the top left corner.

The icons in the status column of the tables show if a checking was 
completed successfully (<span style="color:green">green</span> checkmark),  
interrupted (<span style="color:orange">yellow</span> clock),
not successful (<span style="color:red">red</span> X) or
not executed (<span style="color:darkblue">blue</span> question-mark).

Right-Clicking on one of the items in the tables will open the context-menu, allow you to reload, edit or remove the configurations or formulas. 
Furthermore, the context menu of LTL verification or symbolic checking provides the option to create a counterexample, if a check has failed.

<br>

## <a id="Model"> Model Checking </a>

![Modelchecking](../screenshots/Verifications/Modelchecking.png)

By pressing the plus button you can add several model checking variants. The following view will be shown:

![Modelchecking Stage](../screenshots/Verifications/Modelchecking%20Stage.png)

Select one of the search strategies (breadth first, depth first or a mix of both) and choose, 
which specific errors (e.g. deadlocks, invariants) are supposed to be checked or select a condition that stops the checking process..

By clicking the "Model Check" button, the check will be executed and added to the list in the tab. 
The Description tables gives on overview of the settings you selected before and the status of the model checking. 

The Message table shows the result of the check. When e.g. invariant violations are found, the trace can be displayed in the [history view](History.md). 
Below, there's further information about the checking process.

<br>

## <a id="LTL"> LTL Verifications </a>

![LTL](../screenshots/Verifications/LTL.png)

By clicking the "Add LTL Formula" or "Add LTL Pattern" buttons an editor will be opened, and you can add LTL formulas or patterns to the lists to be checked for.
You can also save created formulas or patterns or load them.

A summary of LTL Syntax and Patterns supported by ProB and how to set fairness constraints, can be found [here](LTL%20Syntax%20and%20Patterns.md).

<br>

## <a id="Symbolic"> Symbolic Checking </a>

![Symbolic Checking](../screenshots/Verifications/Symbolic%20Checking.png)

By clicking the plus button you can add several symbolic checking variants. The following view will be shown:

![Add SC](../screenshots/Verifications/Add%20SC.png)

The dropdown menu allows you to select the type. Some variants of the symbolic checking might need additional parameters.

<br>

## <a id="ProofObligation"> Proof Obligation </a>

The proof obligation is currently under development. This page will be updated, as soon as there are new features available.

[back to top](#top)
