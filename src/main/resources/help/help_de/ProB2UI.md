# Das ProB2 JavaFX Hauptfenster

In der Voreinstellung ist das Hauptfenster in drei vertikale Felder unterteilt (siehe unten).

* Auf der linken Seite, die Operationen-Ansicht, welche die Operationen anzeigt, deren Vor- und Schutzbedingungen in diesem Zustand wahr sind (Die Ansicht benutzt einen blauen Kreispfeil für den Fall, dass eine Operation den Zustand nicht ändert);
* In der Mitte
	* die [Zustandsansicht](Hauptansicht/Zustandsansicht.md), die den aktuellen Zustand der B-Maschine anzeigt und beispielsweise auflistet, welche Werte die Maschienvariablen derzeit haben,
	* die [Zustandsfehleransicht](Hauptansicht/Zustandsfehler.md), welche wohlmöglich auftretende Zustandsfehler anzeigt und 
	* die [Visualisierungsansicht](Hauptansicht/Visualisierung.md), die eine Visualisierung der Zustände anzeigt, falls vom Benutzer zur Verfügung gestellt;
* Im rechten Feld sind einige Unteransichten enthalten, die aktiviert werden können:
	* [Der Verlauf der Operationen, die zu diesem Zustand geführt haben (Verlauf)](Verlauf.md)
	* [Die Projektansicht](Projekt.md)
	* [Die Überprüfungsansicht](Überprüfungen.md)
	* [Die Statistikansicht](Statistik.md)

![ProB2 JavaFX UI Übersicht](screenshots/Übersicht.png)

# Die ProB2 JavaFX Hauptmenüleiste

Die Menüleiste enthält die diversen Kommandos, um auf die Funktionen von ProB zuzugreifen. Sie beinhaltet die Menüs
* Datei,
* Bearbeiten,
* Formel,
* Konsolen,
* Perspektiven,
* Ansicht,
* Plugins,
* Visualisierung,
* Fenster und
* Hilfe

![Dateimenü](screenshots/Menü/Datei.png)

Das Dateiuntermenü erlaubt es ein neues Projekt anzulegen, existierende Projekte oder Maschinen zu öffnen, ein Projekt aus der Liste der zuletzt verwendete Projekte zu öffnen und/oder die Liste der zuletzt verwendeten Projekte zu löschen, das ProB2 JavaFX UI zu schließen, das aktuelle Projekt zu speichern oder die derzeitig laufende Maschine neu zu laden.

![Bearbeiten-Menü](screenshots/Menü/Bearbeiten.png)

Das Bearbeiten-Untermenü stellt zwei Wege, die aktuelle Maschine zu bearbeiten, zur Verfügung (entweder im Editor, der von der ProB2 JavaFX UI gestellt wird, oder im Standardeditor des Betriebssystems) und erlaubt es, die allgemeinen und globalen Präferenzen in einem separaten Fenster zu editieren.

![Formelmenü](screenshots/Menü/Formel.png)

Hier können Formeln zur Visualisierung hinzugefügt sowie das Zeitdiagramm angezeigt werden.

![Konsolenmenü](screenshots/Menü/Konsolen.png)

Dieses Untermenü führt zu zwei Konsolen, eine Groovy, eine B.

![Perspektivenmenü](screenshots/Menü/Perspektiven.png)

Das Perspektivenuntermenü erlaubt es das Aussehen der Hauptansicht zu ändern. Die Standardansicht wird oben angezeigt und zwei zusätzliche Perspektiven (Separater Verlauf und Seperater Verlauf und Statistik) sind vorgegeben. Mit ''Komponenten abtrennen'' kann die Ansicht in einzelnen Fenstern angezeigt werden. ''Laden...'' erlaubt es eigene Perspektiven zu laden, indem man dem Programm eine FXML-Datei, die die Ansichten enthält, zur Verfügung stellt, aber man sollte sich dabei bewusst sein, dass das die Fähigkeit Komponenten abzutrennen zerstören kann.

![Ansichtsmenü](screenshots/Menü/Ansicht.png)

Dieses Untermenü erlaubt es die Schrift- und Buttongröße in dem ProB2 JavaFX UI anzupassen.

![Pluginsmenü](screenshots/Menü/Plugins.png)

Mit dem Pluginsuntermenü kann man Plugins hinzufügen und/oder neu laden sowie ein Fenster öffnen, das alle geladenen Plugins anzeigt.

![Visualisierungsmenü](screenshots/Menü/Visualisierung.png)

Das Visualisierungsuntermenü erlaubt es eine Visualisierung zu wählen, zu stoppen und abzutrennen.

![Hilfemenü](screenshots/Menü/Hilfe.png)

Das Hilfeuntermenü stellt Hilfe zum ProB2 JavaFX UI, Informationen zu dem ProB2 UI, dem ProB2 Kernel, dem ProB CLI und der Javaversion, die hier benutzt werden, und einen Weg, Probleme mit dem ProB2 JavaFX UI zu melden, zur Verfügung.
