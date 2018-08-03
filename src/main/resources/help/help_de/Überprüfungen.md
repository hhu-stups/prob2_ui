# Überprüfungsansicht

Die Überprüfungsansicht liefert 4 verschiedene Methoden eine Maschine zu testen:

* Model-Checking,
* LTL-Überprüfung,
* Symbolic Checking und
* Pfad nachspielen

In jedem Tab kann man multiple Tests hizufügen um die derzeitig ausgewählte Mschine zu testen und den Überprüfungsprozess durch Drücken des "Abbrechen"-Buttons unterbrechen.

## <a id="Model"> Model-Checking </a>

![Model-Checking](../screenshots/Verifications/Modelchecking.png)

Durch Dürcken des Plus-Knopfes kann man diverse Model-Checking-Varianten hinzufügen. Die folgende Ansicht wird gezeigt:

![Model-Checking-Fenster](../screenshots/Verifications/Modelchecking%20Stage.png)

Wählen Sie eine der Suchstrategien (Gemischte Breiten-/Tiefensuche, Breitensuche oder Tiefensuche) und die Checkboxen, die verschiedene mögliche Fehler wie Deadlocks enthalten, um diese zu checken. Durch Drücken des "Model-Check"-Buttons wird die von Ihnen gewählte Variante zur Liste, die oben im Model-Checking-Tab angezeigt wird, hinzugefügt.

## <a id="LTL"> LTL-Überprüfung </a>

![LTL](../screenshots/Verifications/LTL.png)

Durch Drücken von "LTL-Formel hinzufügen" oder "LTL-Pattern hinzufügen" wird ein Editor für das jeweilige geöffnet und man kann LTL-Formeln oder -Patterns zu den Listen hinzufügen, um diese zu checken.

## <a id="Symbolic"> Symbolic Checking </a>

![Symbolic Checking](../screenshots/Verifications/Symbolic%20Checking.png)

Durch Drücken des Plus-Knopfes kann man verschiedene Symbolic Checking-Varianten hinzufügen. Die folgende Ansicht wird angezeigt:

![SC hinzufügen](../screenshots/Verifications/Add%20SC.png)

Das Dropdown-Menü erlaubt es den Typ des Tests auszuwählen. Einige Varianten des Symbolic Checkings verlangen nach zusätzlichen Parametern (z.B. braucht Invariant eine Operation).

## <a id="Trace"> Pfad nachspielen </a>

![Pfad nachspielen](../screenshots/Verifications/Trace%20Replay.png)

Der "Pfad nachspielen"-Tab erlaubt es eine Trace von der Festplatte zu laden und dann von der Maschine nachspielen zu lassen. 
