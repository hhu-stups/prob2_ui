Die Animationsansicht ist in zwei Teile geteilt: Pfad nachspielen und Symbolische Animation.
## <a id="Trace"> Pfad nachspielen </a>

![Pfad](../screenshots/Animation/Replay.png)

Mit diesem Tab kann man Pfade von Maschinen hinzufügen und laden.

Durch drücken des Häkchenknopfes wird jeder ausgewählter Pfad ausgeführt und geprüft ohne den derzeitigen Verlauf an Operationen zu überschreiben. Der X-Knopf kann eine derzeit laufende Ausführung eines Pfades anhalten. Der Ordnerknopf erlaubt es Pfade hinzuzufügen. Der Fragenzeichenknopf öffnet die zum Pfad nachspielen gehörende Hilfeseite.

Wenn man Pfade durch Doppelklicken oder Auswählen von Pfad nachspielen nach einem Rechtsklick überprüft, wird der Verlauf der Operationen durch die im Pfad gespeicherten Operationen überschrieben.

Treten Fehler auf, kann man diese durch Rechtsklick und Auswahl von Fehler anzeigen inspizieren.

Man kann Pfade löschen, indem man rechtklickt und Pfad löschen auswählt.
## <a id="Symbolic"> Symbolic Animation </a> 

![Symbolic](../screenshots/Animation/Symbolic.png)

Bei der symbolischen Animation kann man Animationsbefehle wie `sequence`, `find valid state` oder symbolische
Testfallgenerierung wie `MCDC` und `covered operations` verwenden. Es besteht außerdem die Möglichkeit die generierten
Testfälle zu speichern. Diese werden nach dem Speichern in der Ansicht für das Nachspielen der Pfade angezeigt.

![Sequence](../screenshots/Animation/Sequence.png)

Bei der `sequence` Animation wird eine Sequenz bestehend aus Operationen durch Semikolons getrennt eingegeben.
Hierbei versucht ProB bei der Ausführung die eingegebene Sequenz von Operationen ausgehend von der 
Initialisierung auszuführen.


![FindValidState](../screenshots/Animation/FindValidState.png)

Mit der `find valid state` Animation ist es möglich, Werte für die einzelnen Variablen sowie ein zusätzliches Prädikat
anzugeben. Bei der Ausführung versucht ProB einen erreichbaren Zustand zu finden, wo die Variablen die angegebenen Werte
annehmen und das zusätzliche Prädikat erfüllt ist. Die Animation zeigt letzlich den Pfad von der Initialisierung bis zu
dem gefunden Zustand an.

![CoveredOperations](../screenshots/Animation/CoveredOperations.png)

`Covered operations` generiert dagegen Testfälle, wo die ausgewählten Operationen abgedeckt werden. Jeder Testfall
deckt jeweils eine der ausgewählten Operationen ab und ist ein Trace, der in der Initialisierung beginnt. Der Algorithmus
terminiert, sobald alle ausgewählten Operationen abgedeckt worden sind oder die eingegebene Suchtiefe erreicht ist.


![MCDC](../screenshots/Animation/MCDC.png)

Mit ProB ist es ebenfalls möglich Testfälle nach `MCDC` Kriterien mit dem angegebenen Level zu generieren. Hierbei 
beginnt jeder generierte Testfall ebenfalls in der Initialisierung. Der Algorithmus terminiert, sobald die angegebene
Suchtiefe erreicht ist.


![TraceInformation](../screenshots/Animation/TraceInformation.png)

In der Ansicht über die Testfallgenerierung werden die generierten Testfälle sowie die nicht abgedeckten Operationen
angezeigt. Die Tabelle für die generierten Testfälle beinhaltet Informationen über die Suchtiefe, die Transitionen, die
abgedeckten Operationen sowie dem zugehörigen Guard. Dagegen zeigt die Tabelle für die nicht abgedeckten Operationen die
jeweiligen Operationen mit dem zugehörigen Guard an. Bei einem Doppelklick auf einen nachspielbaren generierten Testfall
wird der aktuelle Pfade auf den Pfad des generierten Testfalls gesetzt.

Das folgende Beispiel zeigt die History nach einem Doppelklick auf den letzten generierten Testfall.

![TestCaseGenerationUpdatedHistory](../screenshots/Animation/TestCaseGenerationUpdatedHistory.png)
