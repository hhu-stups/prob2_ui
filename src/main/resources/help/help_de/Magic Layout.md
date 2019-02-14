# Magic Layout

<img src="../screenshots/MagicLayout.png" alt="Magic Layout Ansicht"
	title="Magic Layout Ansicht Screenshot" width="750" />

Die Magic Layout Ansicht ermöglicht eine interaktive und individualisierbare Zustandsvisualisierung.
Die Ansicht teilt sich auf in die [Graph Ansicht](#graph) auf der rechten Seite und die [Einstellungsansicht](#einstellungen) auf der linken Seite.

## <a name="graph"></a>Graph Ansicht

Der angezeigte Ausschnitt kann durch Zoomen sowie Scrollen angepasst werden. Zum Zoomen werden in der Regel der Plus- und der Minusknopf  verwendet. Bei Verwendung eines Touchfähigen Gerätes, beispielsweise eines Laptops mit Touchpad, kann auch durch eine Zoomgeste mit zwei Fingern hinein- und herausgezoomed werden.

Durch Klicken des Aktualisieren-Knopf werden die gewählten Layout Einstellungen für den Graphen übernommen. Über den Layout-Knopf werden zusätzlich alle Knoten entsprechend des gewählten Layout Algorithmus neu platziert. Derzeit hat der Nutzer die Wahl zwischen einem hierarchischen Layout, bei dem die Knoten in verschiedenen Ebenen untereinander angeordnet werden, und einem zufälligen Layout, bei welchem die Position der Knoten zufällig bestimmt wird. Die Knoten lassen sich zudem per Drag-and-Drop verschieben, sodass auch ein individuelles Graph Layout möglich ist.

## <a name="einstellungen"></a>Einstellungsansicht

Die Einstellungsansicht ist unterteilt in zwei Tabs, eins für Knoten- und eins für Kantengruppen. Beide sind jedoch von der Funktionsweise her grundsätzlich identisch.

Über Datei/Neu/Knotengruppe bzw. Datei/Neu/Kantengruppe, oder über das Kontext Menü der Listenansicht können neue Knoten- bzw. Kantengruppen hinzugefügt werden. Per Doppelklick auf eine bestehende Gruppe, oder über das Kontext Menü einer Gruppe in der Liste, kann der Name dieser Gruppe geändert werden. Es ist nicht möglich, dass zwei Gruppen denselben Namen haben. Ebenso ist es möglich über das Kontext Menü einer bestehenden Gruppe in der Liste diese Gruppe zu löschen. Standardmäßig sind beim Öffnen der Visualisierung Knoten- bzw. Kantenmengen vordeﬁniert, welche die Mengen, Konstanten und Variablen der aktuellen Maschine repräsentieren. Diesen wird jeweils eine zufällige initiale Farbe zugewiesen. 

In dem Text Feld unter der Listen Ansicht wird durch einen B Ausdruck bestimmt, welche Knoten bzw. Kanten die aktuell ausgewählte Knoten- bzw. Kantengruppe umfasst. 

Mit Hilfe der Auswahl Knöpfe unter dem Textfeld können dann die gewünschten Einstellungen für die aktuell ausgewählte Knote- bzw. Kantengruppe ausgewählt werden. Die Einstellungen werden erst übernommen, wenn der Graph über die entsprechenden Knöpfe in der [Graph Ansicht](#graph) aktualisiert oder neu gelayouted wird.

Die Reihenfolge, in welcher die deﬁnierten Einstellungen angewendet werden, wird durch die Reihenfolge bestimmt, in der die deﬁnierten Knoten- bzw. Kantengruppen in der Listenansicht aufgelistet sind. Je weiter unten eine Knoten- bzw. Kantenmenge aufgeführt ist, desto später werden die Einstellungen auf den Graphen angewendet. Neue Knoten- bzw. Kantengruppen werden zunächst ganz unten in der Liste platziert. Die Reihenfolge lässt sich per Drag-and-Drop anpassen.

Über Datei/Layout Einstellungen speichern... bzw. Datei/Layout Einstellungen laden... können vorgenommene Einstellungen gespeichert sowie geladen werden.
Der Graph kann über Datei/Graph als Bild speichern außerdem als Bild-Datei gespeichert werden.