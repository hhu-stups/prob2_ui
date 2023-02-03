# <a name="top"></a>

Die Animationsansicht ist in drei Teile geteilt: [Nachspielen](#Trace), [Symbolisch](#Symbolic) und [Testfallgenerierung](#testCases).
Jede hinzugefügte Animation wird, ggf. mit der gewählten Konfiguration in der Tabelle angezeigt.
Der Status zeigt an, ob eine Animation erfolgreich (<span style="color:green">grünes</span> Häkchen) oder nicht erfolgreich (<span style="color:red">rotes</span> X) nachgespielt wurde,
oder ob der Status unbekannt ist (<span style="color:darkblue">blaues</span> Fragezeichen).
Im Kontextmenü der einzelnen Einträge können diese bearbeitet, entfernt oder neu überprüft werden sowie weitere Informationen angezeigt werden.

<br>

## <a id="Trace"> Nachspielen </a>

![Pfad](../screenshots/Animation/Replay.png)

Mit diesem Tab kann man Pfade von Maschinen laden und nachspielen.

Durch Drücken des Häkchen-Buttons wird jeder ausgewählter Pfad ausgeführt und geprüft, ohne den derzeitigen Verlauf an Operationen zu überschreiben. Dies kann mit dem
x-Button unterbrochen werden. Weitere Pfade können mithilfe des Ordner-Buttons hinzugefügt werden.
Der Kreispfeil läd die gesamte Maschine neu und setzt hierdurch den Status der Pfade zurück.

Wenn man Pfade durch Doppelklicken oder im Kontextmenü durch "Pfad nachspielen" überprüft, wird der Verlauf der Operationen durch die im Pfad gespeicherten Operationen überschrieben.
Im Kontextmenü eines einzelnen Pfades können zusätzlich einzelne Transitionen des Pfades bearbeitet werden.

<br>

## <a id="Symbolic"> Symbolisch </a>

![Symbolic](../screenshots/Animation/Symbolic.png)

Bei der symbolischen Animation kann man mithilfe des Plus-Buttons Animationsbefehle wie `Sequenz`, `Gültigen Zustand suchen` verwenden. 

![Sequence](../screenshots/Animation/Sequence.png)

Bei der `Sequenz` Animation wird eine Sequenz bestehend aus Operationen durch Semikolons getrennt eingegeben.
Hierbei versucht ProB bei der Ausführung die eingegebene Sequenz von Operationen ausgehend von der 
Initialisierung auszuführen.


![FindValidState](../screenshots/Animation/FindValidState.png)

Mit der `Gültigen Zustand Suchen` Animation ist es möglich, Werte für die einzelnen Variablen sowie ein zusätzliches Prädikat
anzugeben. Bei der Ausführung versucht ProB einen erreichbaren Zustand zu finden, in dem die Variablen die angegebenen Werte
annehmen und das zusätzliche Prädikat erfüllt ist. Die Animation zeigt letztlich den Pfad von der Initialisierung bis zu
dem gefundenen Zustand an.

<br>

## <a id="testCases"> Testfallgenerierung </a>

Bei der Testfallgenerierung können Testfälle erzeugt werden, die MCDC Kriterien entsprechen oder die ausgewählte Operationen abdecken.
Es besteht außerdem die Möglichkeit die generierten Testfälle zu speichern. Diese werden nach dem Speichern in der Ansicht für das Nachspielen der Pfade angezeigt.

![CoveredOperations](../screenshots/Animation/CoveredOperations.png)

`Covered Operations Testen` generiert Testfälle, in denen die ausgewählten Operationen abgedeckt werden. Jeder Testfall
deckt jeweils eine der ausgewählten Operationen ab und ist ein Trace, der in der Initialisierung beginnt. Der Algorithmus
terminiert, sobald alle ausgewählten Operationen abgedeckt worden sind oder die eingegebene Suchtiefe erreicht ist.


![MCDC](../screenshots/Animation/MCDC.png)

Mit ProB ist es ebenfalls möglich Testfälle nach `MCDC` Kriterien mit dem angegebenen Level zu generieren. Hierbei 
beginnt jeder generierte Testfall ebenfalls in der Initialisierung. Der Algorithmus terminiert, sobald die angegebene
Suchtiefe erreicht ist.


![TraceInformation](../screenshots/Animation/TraceInformation.png)

Mit einem Rechtsklick auf den Testfall ist es möglich, die Details des Testfalls anzeigen zu lassen.
Die Tabelle für die generierten Testfälle beinhaltet Informationen über die Suchtiefe, den Pfad, die
abgedeckten Operationen sowie den zugehörigen Guard. Dagegen zeigt die Tabelle für die nicht abgedeckten Operationen die
jeweiligen Operationen mit dem zugehörigen Guard an. Bei einem Doppelklick auf einen nachspielbaren generierten Testfall
wird der aktuelle Pfad auf den Pfad des generierten Testfalls gesetzt.

Das folgende Beispiel zeigt die History nach einem Doppelklick auf den letzten generierten Testfall.

![TestCaseGenerationUpdatedHistory](../screenshots/Animation/TestCaseGenerationUpdatedHistory.png)


[Nach oben](#top)
