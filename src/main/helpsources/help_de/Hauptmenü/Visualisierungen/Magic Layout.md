
![Magic Layout](../../../screenshots/Visualisations/MagicLayout.png)


Die Magic Layout Ansicht ermöglicht eine interaktive und individualisierbare Zustandsvisualisierung.
Die Ansicht teilt sich auf in die [Einstellungsansicht](#einstellungen) auf der linken Seite und die [Graph Ansicht](#graph) auf der rechten Seite.

## <a name="einstellungen"></a>Einstellungsansicht

Die Einstellungsansicht ist unterteilt in zwei Tabs, eins für Knoten- und eins für Kantengruppen. Beide sind jedoch von der Funktionsweise her grundsätzlich identisch.

Über Datei/Neu/... oder über das Kontext-Menü der Listenansicht können neue Knoten- bzw. Kantengruppen hinzugefügt werden. 

Per Doppelklick auf eine bestehende Gruppe oder über das Kontext-Menü einer Gruppe in der Liste kann der Name dieser Gruppe geändert werden. 

Ebenso ist es möglich, über das Kontext-Menü einer Gruppe diese zu löschen. Standardmäßig sind beim Öffnen der Visualisierung Knoten- bzw. Kantenmengen vordeﬁniert, 
welche die Mengen, Konstanten und Variablen der aktuellen Maschine repräsentieren. Diesen wird jeweils ein zufälliges Design zugewiesen, 
welches über die DropDown-Menüs in der unteren linken angepasst werden kann. Um die Auswahl zu übernehmen, muss die Visualisierung über den Button in der Graph-Ansicht
aktualisiert werden.

In dem Textfeld unter der Listenansicht wird durch einen B Ausdruck bestimmt, welche Knoten bzw. Kanten die aktuell ausgewählte Knoten- bzw. Kantengruppe umfasst.

Die Reihenfolge, in welcher die deﬁnierten Einstellungen angewendet werden, wird durch die Reihenfolge bestimmt, 
in der die deﬁnierten Knoten- bzw. Kantengruppen in der Listenansicht aufgelistet sind. Je weiter unten eine Knoten- bzw. Kantenmenge aufgeführt ist, desto später werden die Einstellungen auf den Graphen angewendet. 
Neue Knoten- bzw. Kantengruppen werden zunächst ganz unten in der Liste platziert. Die Reihenfolge lässt sich per Drag-and-Drop anpassen.

Über das Datei-Menü am oberen Bildschirmrand können Layout-Einstellungen gespeichert und geladen werden, sowie der Graph als Bild-Datei abgespeichert werden.


## <a name="graph"></a>Graph Ansicht

Die Graph-Ansicht zeigt die Visualisierung des Zustands. 

Der angezeigte Ausschnitt kann durch Zoomen sowie Scrollen angepasst werden. Zum Zoomen werden in der Regel der Plus- und der Minusknopf  verwendet. 
Bei Verwendung eines Touchfähigen Gerätes, beispielsweise eines Laptops mit Touchpad, kann auch durch eine Zoomgeste mit zwei Fingern hinein- und herausgezoomed werden.

Über den Layout-Button werden alle Knoten entsprechend des gewählten Layout-Algorithmus neu platziert. Derzeit hat der Nutzer die Wahl zwischen zwei Layouts:
* hierarchischen: die Knoten werden in verschiedenen Ebenen untereinander angeordnet
* zufällig: die Position der Knoten wird zufällig bestimmt

Die Knoten lassen sich zudem per Drag-and-Drop verschieben, sodass auch ein individuelles Graph Layout möglich ist.

