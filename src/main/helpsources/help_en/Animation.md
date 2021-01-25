The Animation View is divided in two sections: Trace Replay and Symbolic Animation.
## <a id="Trace"> Trace Replay </a>

![Trace](../screenshots/Animation/Replay.png)

With this tab you can add and load traces of machines.

By hitting the check button every selected trace will be executed and checked without rewriting your current history of operations. The x button will cancel an ongoing execution of a trace. The folder button allows you to add traces. The questionsmark button will open the help page regarding Trace Replay.

If you check your traces by double clicking or selecting Replay Trace after clicking right on a trace, the history of operations will be overwritten by the operations stored in the trace.

If errors occurr, you can inspect them by clicking right and selecting Show Error.

You can remove traces by clicking right and select Delete Trace.
## <a id="Symbolic"> Symbolic Animation </a> 

![Symbolic](../screenshots/Animation/Symbolic.png)

In Symbolic Animation you can use animation commands such as `sequence`, `find valid state` and symbolic test case
generation commands such as `MCDC` and `covered operations`. Furthermore, it is possible to save the generated test
cases. After saving the generated test cases, they will be shown in the trace replay view.

![Sequence](../screenshots/Animation/Sequence.png)

Using `sequence` animation, it is possible to type in a sequence consisting of operations separated by a semicolon.
When executing this animation, ProB tries to execute the given sequence of operations starting from the initialisation.

![FindValidState](../screenshots/Animation/FindValidState.png)

With the `find valid state` animation, you can define the values of the variables and an additional predicate. 
ProB will then try to find a reachable state where the variables hold the given values and the additional predicate 
is true. The animation also replays the path from the initialisation to the found state.

![CoveredOperations](../screenshots/Animation/CoveredOperations.png)

Executing `covered operations` generates test cases where the selected operations are covered. Each generated test
case covers one selected operation and is a trace starting from the initialisation. The algorithm stops when all selected 
operations are covered or the maximum search depth is reached.

![MCDC](../screenshots/Animation/MCDC.png)

ProB also provides the possibility to generate test cases following the `MCDC` criterion with the given level.
Each generated test case starts from the initialisation. The algorithm stops when the maximum search depth is reached. 

![TraceInformation](../screenshots/Animation/TraceInformation.png)

The trace information view shows an overview of the generated test cases and the uncovered operations. The table for the
generated test cases contains information about the search depth, the transitions, the covered operation and the belonging
guard. Again, the table for the uncovered operations contains the operations and its guard only. Double-clicking on a
replayable generated test case sets the current trace to the selected test case.

The example below shows the resulted history after double-clicking on the last test case in the trace information view.

![TestCaseGenerationUpdatedHistory](../screenshots/Animation/TestCaseGenerationUpdatedHistory.png)