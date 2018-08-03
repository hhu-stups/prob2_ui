# Das ProB2 JavaFX Hauptfenster

In der Voreinstellung ist das Hauptfenster in drei vertikale Felder unterteilt (siehe unten).

* Auf der linken Seite, die [Operationen-Ansicht](Operationen.md), welche die Operationen anzeigt, deren Vor- und Schutzbedingungen in diesem Zustand wahr sind (Die Ansicht benutzt einen blauen Kreispfeil für den Fall, dass eine Operation den Zustand nicht ändert);
* In der Mitte
	* die [Zustandsansicht](Hauptansicht/Zustandsansicht.md), die den aktuellen Zustand der B-Maschine anzeigt und beispielsweise auflistet, welche Werte die Maschienvariablen derzeit haben,
	* die [Zustandsfehleransicht](Hauptansicht/Zustandsfehler.md), welche wohlmöglich auftretende Zustandsfehler anzeigt und 
	* die [Zustandsvisualisierungsansicht](Hauptansicht/Zustandsvisualisierung.md), die eine Visualisierung der Zustände anzeigt, falls vom Benutzer zur Verfügung gestellt;
* Im rechten Feld sind einige Unteransichten enthalten, die aktiviert werden können:
	* [Der Verlauf der Operationen, die zu diesem Zustand geführt haben (Verlauf)](Verlauf.md)
	* [Die Projektansicht](Projekt.md)
	* [Die Überprüfungsansicht](Überprüfungen.md)
	* [Die Statistikansicht](Statistik.md)

![ProB2 JavaFX UI Übersicht](../screenshots/Overview.png)

[Mehr über ProB](https://www3.hhu.de/stups/prob/index.php/Main_Page)

# Die ProB2 JavaFX Hauptmenüleiste

Die Menüleiste enthält die diversen Kommandos, um auf die Funktionen von ProB zuzugreifen. Sie beinhaltet die Menüs
* Datei,
* Ansicht,
* Visualisierung,
* Erweitert,
* Fenster und
* Hilfe

![Dateimenü](../screenshots/Menu/File.png)

Das Dateiuntermenü erlaubt es ein neues Projekt anzulegen, existierende Projekte oder Maschinen zu öffnen, ein Projekt aus der Liste der zuletzt verwendete Projekte zu öffnen und/oder die Liste der zuletzt verwendeten Projekte zu löschen, das ProB2 JavaFX UI zu schließen, das aktuelle Projekt zu speichern, die derzeitig laufende Maschine neu zu laden oder Einstellungen zu editieren.

![Ansichtsmenü](../screenshots/Menu/View.png)

Dieses Untermenü erlaubt es die Schrift- und Buttongröße in dem ProB2 JavaFX UI anzupassen oder in den Fullscreen-Modus zu gehen. 

![Visualisierungsmenü](../screenshots/Menu/Visualisation.png)

Das Visualisierungsuntermenü stellt verschiedene Formen der Visualisierung zur Verfügung. Man kann eine Formel eingeben, sich das Zeitdiagramm anschauen oder einen Blick auf die Graphvisualisierung werfen.

![Weitere-Optionen-Menü](../screenshots/Menu/Advanced.png)

Dieses Untermenü stellt eine Groovy-Konsole zur Verfügung und erlaubt es Plugins und eigene Visualisierungen zu verwalten.

![Fenster-Menü](../screenshots/Menu/Window.png)

Das Fensteruntermenü erlaubt es Perspektiven zu ändern und Komponenten abzutrennen. Dieses UI bringt 3 verschiedene voreingestellte Perspektiven. Es ist erlaubt eigene Perspektiven zu nutzen, indem man eine FXML-Datei, die die Ansichten enthält, zur Verfügung stellt, aber man sollte sich dabei bewusst sein, dass das die Fähigkeit Komponenten abzutrennen zerstören kann. [Erstellung von eigenen FXML-Dateien](Perspektiven.md)

![Hilfemenü](../screenshots/Menu/Help.png)

Das Hilfeuntermenü stellt Hilfe zum ProB2 JavaFX UI, Informationen zu dem ProB2 UI, dem ProB2 Kernel, dem ProB CLI und der Javaversion, die hier benutzt werden, und einen Weg, Probleme mit dem ProB2 JavaFX UI zu melden, zur Verfügung.
