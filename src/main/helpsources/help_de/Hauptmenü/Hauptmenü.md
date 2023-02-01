# <a name="top"></a>
# Die Hauptmenüleiste

Die Menüleiste enthält die diversen Kommandos, um auf die Funktionen von ProB zuzugreifen. Sie beinhaltet die Menüs
* [Datei](#Datei)
* [Ansicht](#Ansicht),
* [Visualisierung](#Visualisierung),
* [Erweitert](#Erweitert),
* [Fenster](#Fenster) und
* [Hilfe](#Hilfe)


## <a name="Datei"> Datei</a>
![Dateimenü](../../screenshots/Menu/File.png)

Das Dateiuntermenü erlaubt es ein neues [Projekt](../Projekt.md) anzulegen, das aktuell geöffnete Projekt zu speichern und neu zu laden 
oder die Liste der zuletzt geöffneten Projekte zu verwalten.

Die derzeitig laufende Maschine kann exportiert werden oder ihre interne Repräsentation angezeigt werden.

Im Untermenü "Einstellungen" können die Darstellung des UI und die Präferenzen des aktuellen Projekts geändert werden.
"Schließen" beendet das Programm.


## <a name="Ansicht"> Ansicht</a>
![Ansichtsmenü](../../screenshots/Menu/View.png)

Dieses Untermenü erlaubt es die Schrift- und Buttongröße in dem ProB2 JavaFX UI anzupassen oder in den Fullscreen-Modus zu gehen.


## <a name="Visualisierung"> Visualisierung</a>
![Visualisierungsmenü](../../screenshots/Menu/Visualisation.png)


Das Visualisierungsuntermenü stellt verschiedene Formen der Visualisierung zur Verfügung.

Es ist möglich, die Transitionen und den Zustand der Maschine als [Graph](Visualisierungen/Graphvisualisierung.md),
als [Tabelle](Visualisierungen/Tabellenvisualisierung.md) oder mithilfe des [Magic Layout](Visualisierungen/Magic%20Layout.md) zu visualisieren.

Durch [VisB öffnen](Visualisierungen/VisB.md) bietet eine weitere Visualisierungsmöglichkeit, wenn im Projekt passende .json und .svg-Dateien zur Verfügung stehen.

Des Weiteren bietet das [Zeitdiagramm](Visualisierungen/Zeitdiagramm.md) eine Darstellung bestimmter Werte anhand der eingegebenen Formeln.


## <a name="Erweitert"> Erweitert</a>
![Weitere-Optionen-Menü](../../screenshots/Menu/Advanced.png)


Dieses Untermenü stellt eine Groovy- sowie eine ProB-Kern-Konsole zur Verfügung und erlaubt es Plugins und eigene Visualisierungen zu verwalten.

Die [SimB]-Option stellt Simulationen für formale Modellierung zur Verfügung.


## <a name="Fenster"> Fenster</a>
![Fenster-Menü](../../screenshots/Menu/Window.png)


Das Fensteruntermenü erlaubt es Perspektiven zu ändern und [Komponenten abzutrennen](Fenster/Abtrennen%20von%20Komponenten.md).

Dieses UI bringt 3 verschiedene voreingestellte Perspektiven. Es ist erlaubt [eigene Perspektiven](Fenster/Perspektiven.md) zu nutzen, indem man eine FXML-Datei,
die die Ansichten enthält, zur Verfügung stellt. Achtung! Die Nutzung eigener Perspektiven kann dazu führen, dass Komponenten nicht mehr abgetrennt werden können.

## <a name="Hilfe"> Hilfe</a>
![Hilfemenü](../../screenshots/Menu/Help.png)


Das Hilfeuntermenü stellt Hilfe zum ProB2 JavaFX UI und Informationen zum ProB2 UI (inklusive Details zum ProB2 Kernel, dem ProB CLI und der Javaversion, die hier benutzt werden) zur Verfügung.

Des Weiteren können Probleme und Bugs gemeldet werden. Im Menü Syntax wird eine Übersicht über die Syntax verschiedener unterstützter Sprachen, wie B, TLA oder CSP zu Verfügung gestellt.

[Zum Seitenanfang](#top)
