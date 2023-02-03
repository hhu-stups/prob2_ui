# <a name="top"></a>
The Animation View is divided in three sections: [Replay](#Trace), [Symbolic](#Symbolic) and [Test Case Generation](#testCases).
Each animation that is added will be shown in the table with the chosen configuration.
The state shows, if the animation was successfully (<span style="color:green">green</span> checkmark) or not successfully (<span style="color:red">red</span> X) replayed,
or if the status is unknown (<span style="color:darkblue">blue</span> questionmark).
The contextmenu of each entry allows you to edit, remove or replay the trace or show further information.

<br>

## <a id="Trace"> Replay </a>

![Trace](../screenshots/Animation/Replay.png)

With this tab you can add and load traces of machines.

By hitting the check button every selected trace will be executed and checked without rewriting your current history of operations. 
The x button will cancel an ongoing execution of a trace. The folder button allows you to add traces. 

If you check your traces by double-clicking or selecting Replay Trace after clicking right on a trace, 
the history of operations will be overwritten by the operations stored in the trace.

The circular arrow reloads the current machine and resets the status of the traces.

In the contextmenu of a trace, each transition can be edited.

<br>

## <a id="Symbolic"> Symbolic</a> 

![Symbolic](../screenshots/Animation/Symbolic.png)

In Symbolic Animation you can use animation commands such as `sequence`, `find valid state`. 

![Sequence](../screenshots/Animation/Sequence.png)

Using `sequence` animation, it is possible to type in a sequence consisting of operations separated by a semicolon.
When executing this animation, ProB tries to execute the given sequence of operations starting from the initialisation.

![FindValidState](../screenshots/Animation/FindValidState.png)

With the `find valid state` animation, you can define the values of the variables and an additional predicate. 
ProB will then try to find a reachable state where the variables hold the given values and the additional predicate 
is true. The animation also replays the path from the initialisation to the found state.

<br>

## <a id="testCases"> Test Case Generation </a>

By Test Case Generation you can generate testcases which follow the MCDC criterion or cover selected operations. 
Furthermore, it is possible to save the generated test cases. After saving, they will be shown in the Replay view.

![CoveredOperations](../screenshots/Animation/CoveredOperations.png)

Executing `covered operations` generates test cases where the selected operations are covered. Each generated test
case covers one selected operation and is a trace starting from the initialisation. The algorithm stops when all selected 
operations are covered or the maximum search depth is reached.

![MCDC](../screenshots/Animation/MCDC.png)

ProB also provides the possibility to generate test cases following the `MCDC` criterion with the given level.
Each generated test case starts from the initialisation. The algorithm stops when the maximum search depth is reached. 

![TraceInformation](../screenshots/Animation/TraceInformation.png)

Right-clicking on a testcase shows the option "Show details", which displays the Trace Information. This view shows an overview of the generated test cases and the uncovered operations. The table for the
generated test cases containing information about the search depth, the trace, the covered operation and the belonging
guard. Again, the table for the uncovered operations contains the operations and its guard only. Double-clicking on a
replayable generated test case sets the current trace to the selected test case.

The example below shows the resulted history after double-clicking on the last test case in the trace information view.

![TestCaseGenerationUpdatedHistory](../screenshots/Animation/TestCaseGenerationUpdatedHistory.png)

[back to top](#top)
