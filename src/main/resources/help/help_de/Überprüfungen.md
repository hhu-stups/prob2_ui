# Überprüfungsansicht

Die Überprüfungsansicht liefert 3 verschiedene Methoden eine Maschine zu testen:

* Model-Checking,
* LTL-Überprüfung und
* Symbolic Checking

In jedem Tab kann man multiple Tests hizufügen um die derzeitig ausgewählte Mschine zu testen und den Überprüfungsprozess durch Drücken des "Abbrechen"-Buttons unterbrechen.

## <a id="Model"> Model-Checking </a>

![Model-Checking](../screenshots/Verifications/Modelchecking.png)

Durch Dürcken des Plus-Knopfes kann man diverse Model-Checking-Varianten hinzufügen. Die folgende Ansicht wird gezeigt:

![Model-Checking-Fenster](../screenshots/Verifications/Modelchecking%20Stage.png)

Wählen Sie eine der Suchstrategien (Gemischte Breiten-/Tiefensuche, Breitensuche oder Tiefensuche) und die Checkboxen, die verschiedene mögliche Fehler wie Deadlocks enthalten, um diese zu checken. Durch Drücken des "Model-Check"-Buttons wird die von Ihnen gewählte Variante zur Liste, die oben im Model-Checking-Tab angezeigt wird, hinzugefügt.

## <a id="LTL"> LTL-Überprüfung </a>

![LTL](../screenshots/Verifications/LTL.png)

Durch Drücken von "LTL-Formel hinzufügen" oder "LTL-Pattern hinzufügen" wird ein Editor für das jeweilige geöffnet und man kann LTL-Formeln oder -Patterns zu den Listen hinzufügen, um diese zu checken.

### Zusammenfassung der von ProB unterstützten LTL Syntax
*   Schreibe {...} für B Prädikate,
*   G,F,X,U,W,R,true,false,not,&,or and => sind in der unterstützten LTL Syntax enthalten,
*   Schreibe e(op) umzu überprüfen. ob die Operation op is verfügbar ist,
*   Schreibe deadlock umzu überprüfen ob der Zustand gedeadlockt ist,
*   Schreibe deadlock(op1,...,opk) mit k>0 umzu überprüfen ob alle Operationen opi nicht verfügbar sind,
*   Schreibe controller(op1,...,opk) mit k>0 umzu prüfen, ob es genau eine Operation opi existiert, die verfügbar ist,
*   Schreibe deterministic(op1,...,opk) mit k>0 umzu überprüfen, ob alle Operationen opi determinstisch sind,
*   Schreibe sink umzu überprüfen, ob keine Operation verfügbar ist, dessen Ausführung in einen anderen Zustand führt,
*   Schreibe [], umzu überprüfen was die nächste Operation ist z.B. [reset] => X{db={}},
*   Past-LTL wird ebenfalls unterstützt: Y,H,O,S,T sind dual zu X,G,F,U,R.

#### Fairness Constraints
*   Schreibe Fairness Constraints als Implikation: fair => f, wobei "fair" die Fairness Constraints sind und "f" die zu überprüfende LTL Formel,
*   Schreibe WF(-) und SF(-) um aktionsbasierte schwache bzw. aktionsbasierte starke Fairness Constraints überprüfen,
*   Schreibe WEF und SEF um schlechte Pfade zu suchen, die schwach fair bzw. stark fair bezüglich allen Transitionen ist

## <a id="Symbolic"> Symbolic Checking </a>

![Symbolic Checking](../screenshots/Verifications/Symbolic%20Checking.png)

Durch Drücken des Plus-Knopfes kann man verschiedene Symbolic Checking-Varianten hinzufügen. Die folgende Ansicht wird angezeigt:

![SC hinzufügen](../screenshots/Verifications/Add%20SC.png)

Das Dropdown-Menü erlaubt es den Typ des Tests auszuwählen. Einige Varianten des Symbolic Checkings verlangen nach zusätzlichen Parametern (z.B. braucht Invariant eine Operation).
