Die Überprüfungsansicht liefert vier verschiedene Methoden eine Maschine zu testen:

* [Modelchecking](#Model),
* [LTL Überprüfung](#LTL),
* [Symbolische Überprüfung](#Symbolic) und
* [Beweisobligation](#ProofObligation)

In jedem Tab kann man mehrere Tests hinzufügen, um die derzeitig ausgewählte Maschine zu testen.
Der Überprüfungsprozess kann durch Drücken des "Abbrechen"-Buttons am linken oberen Rand der Ansicht unterbrochen oder neu gestartet werden.

Die Symbole in der Status-Anzeige der Tabelle geben an, ob eine Überprüfung erfolgreich war (<span style="color:green">grünes</span> Häkchen), 
unterbrochen wurde (<span style="color:orange">gelb</span> Uhr), nicht erfolgreich war (<span style="color:red">rotes</span> X) 
oder (noch) nicht ausgeführt wurde (<span style="color:darkblue">blaues</span> Fragezeichen).
  
Das Kontextmenü der Tabelleneinträge ermöglicht das Neustarten, Bearbeiten oder Entfernen einer Konfiguration oder einer Formel.
Außerdem kann man mithilfe des Kontextmenüs einer symbolischen oder einer LTL Überprüfung ein Gegenbeispiel erzeugen lassen, wenn die Überprüfung fehlgeschlagen ist. 

<br>

## <a id="Model"> Modelchecking </a>

![Model-Checking](../screenshots/Verifications/Modelchecking.png)

Durch Drücken des Plus-Knopfes kann man diverse Model-Checking-Varianten hinzufügen. Die folgende Ansicht wird gezeigt:

![Model-Checking-Fenster](../screenshots/Verifications/Modelchecking%20Stage.png)

Wählen Sie eine der Suchstrategien (Gemischte Breiten-/Tiefensuche, Breitensuche oder Tiefensuche) sowie mittels der Checkboxen, 
auf welche möglichen Fehler (z.B. Deadlocks, Invarianten) geprüft werden soll oder unter welcher Bedingung die Überprüfung beendet werden soll. 
Durch Drücken des "Model-Check"-Buttons wird die gewählte Variante ausgeführt und zur Liste, die oben im Model-Checking-Tab angezeigt wird, hinzugefügt.

Die Tabelle "Beschreibung" zeigt eine Übersicht über die zuvor ausgewählten Einstellungen und den Status der Überprüfung.
Die Tabelle "Nachricht" zeigt das Ergebnis der Überprüfung. Wenn beispielsweise eine Invariante verletzt wurde, kann der Pfad, der dorthin geführt hat, 
in der [Verlaufs-Übersicht](Verlauf.md) angezeigt werden.

<br>

## <a id="LTL"> LTL-Überprüfung </a>

![LTL](../screenshots/Verifications/LTL.png)

Durch Drücken von "LTL-Formel hinzufügen" oder "LTL-Pattern hinzufügen" wird ein Editor geöffnet 
und man kann LTL-Formeln bzw. -Patterns zu den Listen hinzufügen, um diese zu überprüfen.
Ebenso kann man bereits erstellte Formeln oder Muster laden oder speichern.

Eine Zusammenfassung der LTL Syntax und Muster, die von ProB unterstützt werden, ist [hier](LTL%20Syntax%20und%20Muster.md) zu finden.

<br>


## <a id="Symbolic"> Symbolic Checking </a>

![Symbolic Checking](../screenshots/Verifications/Symbolic%20Checking.png)

Durch Drücken des Plus-Knopfes kann man verschiedene Symbolic Checking-Varianten hinzufügen. Die folgende Ansicht wird angezeigt:

![SC hinzufügen](../screenshots/Verifications/Add%20SC.png)

Das Dropdown-Menü erlaubt es den Typ des Tests auszuwählen. Einige Varianten der symbolischen Überprüfung benötigen zusätzliche Parameter.

<br>

## <a id="ProofObligation"> Beweisobligation </a>

Die Beweisobligation wird derzeit entwickelt. Sobald neue Features verfügbar sind, wird diese Seite aktualisiert.


[nach oben](#top)
