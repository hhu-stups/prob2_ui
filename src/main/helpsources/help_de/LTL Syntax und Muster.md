([zurück zu Überprüfungen](Überprüfungen.md))

## Zusammenfassung der von ProB unterstützten LTL Syntax


| Ausdruck                             | Funktion                                                                        |
|--------------------------------------|---------------------------------------------------------------------------------|
| {...}                                | enthält die B-Prädikate                                                         |
| e(op)                                | überprüft, ob die Operation op verfügbar ist                                    |
| deadlock                             | überprüft, ob der Zustand gedeadlockt ist,                                      |
| deadlock(op1,...,opk)  (mit k>0)     | überprüft, ob alle Operationen in den Klammern nicht verfügbar ist              |
| controller(op1,...,opk) (mit k>0)    | überprüft, ob maximal eine der Operationen in den Klammern verfügbar ist        |
| deterministic(op1,...,opk) (mit k>0) | überprüft, ob höchstens eine der Operationen in den Klammern verfügbar ist      |
| sink                                 | überprüft, ob keine Operation verfügbar ist, die in einen anderen Zustand führt |
| brackets                             | überprüft, was die nächste Operation ist z.B. [reset] => X{db={}}               |

*   G,F,X,U,W,R,true,false,not,&,or and => sind in der unterstützten LTL Syntax enthalten,
*   Past-LTL wird ebenfalls unterstützt: Y,H,O,S,T sind dual zu X,G,F,U,R.

<br>

#### Fairness Constraints
*   Schreibe Fairness Constraints als Implikation: fair => f, wobei "fair" die Fairness Constraints sind und "f" die zu überprüfende LTL Formel,
*   Schreibe WF(-) und SF(-) um aktionsbasierte schwache bzw. aktionsbasierte starke Fairness Constraints überprüfen,
*   Schreibe WEF und SEF um schlechte Pfade zu suchen, die schwach fair bzw. stark fair bezüglich allen Transitionen ist
	
<br>

## Zusammenfassung der von ProB unterstützten LTL Muster

* Vorhandene Typen: num (Nicht negative ganze Zahlen), seq (Sequenzen), var (LTL Formel)
* Definition von Variablen: `<Typ>` `<Bezeichner>`: `<Wert>`
* Zuweisung von Variablen: `<Bezeichner>`: `<Wert>`
* Scopes für Variablen: Schleife, Muster, Global (bei der Benutzung einer Variable wird von einem lokale Scope immer ein Scope weiter nach außen geschaut, bis die passende Variable gefunden ist)
* Musterdefinition:


```
	def <Name> ( <Parameterliste> ):
		<Rumpf>
```

> Die Parameterliste enthält Parameter, die von folgender Form sind:
* `<Bezeichner>` : `<Typ-Bezeichner>` für Variablen vom Typ num und seq
* `<Bezeichner>` für Variablen vom Typ var
> Die Parameter sind durch Kommata getrennt. Der Rumpf kann Variablen definieren und zuweisen und Schleifen enthalten. Das Überladen von Mustern ist erlaubt.

* Musteraufruf: `<Name>`( `<Argument-Liste>` )

> Bemerkung: Musteraufrufe sind unabhängig von der Reihenfolge der Definition. D.h. ein Musteraufruf ist möglich bevor ein Muster definiert worden ist. Mustern können nur in dem globalen Scope definiert werden.
Es ist also nicht möglich Mustern innerhalb von Mustern zu definieren.

* Schleifen:


```
	count <Bezeichner>: <Startwert> <up/down> to <Endwert>:
		<Rumpf>
	end
```

>Der Rumpf muss mindestens eine Variable definieren oder zuweisen.

* Sequenz-Definition: ( `<Formel-Liste>` )

>Eine Sequenz ist eine Abfolge von mehreren verschiedenen Formeln, die in unterschiedlichen Zuständen gelten. Dabei müssen die Zustände nicht direkt aufeinander folgen.
Die `<Formel-Liste>` muss aus mindestens zwei Formeln bestehen

* Sequenz-Definition mit Zusatzbedingungen:

>( `<Formel-Liste>` without `<Bedingung>` )
>`<Bezeichner>` without `<Bedingung>`

>Die erste Schreibweise ist eine Erweiterung der einfachen Sequenz-Definition um die Zusatzbedingung. Bei der zweiten Schreibweise ist der `<Bezeichner>` der Name einer Variablen mit Datentyp seq.
Die Sequenz, die in der Variablen gespeichert ist, wird hier verwendet um eine neue Sequenz mit der Zusatzbedingung zu definieren.

* Aufruf einer Sequenz: seq( `<Argument>` )

>Hierbei ist `<Argument>` eine Sequenzvariable oder eine Sequenz-Definition. Der Aufruf gibt eine gültige LTL Formel zurück.

* Gültigkeitsbereiche:

>before(`<Rechter Begrenzer>`, `<Eigenschaft>`)
after(`<Linker Begrenzer>`, `<Eigenschaft>`)
between(`<Linker Begrenzer>`, `<Rechter Begrenzer>`, `<Eigenschaft>`)
after_until(`<Linker Begrenzer>`, `<Rechter Begrenzer>`, `<Eigenschaft>`)

>Die Begrenzer- und Eigenschafts-Formeln müssen jeweils gültige LTL Formeln sein. Der Rückgabewert der einzelnen Gültigkeitsbereiche ist jeweils eine gültige LTL Formel, die die angegebene Eigenschaft im jeweiligen Bereich repräsentiert.


* Einzeilige Kommentare : // Kommentar
* Mehrzeilige Kommentare: /* Kommentar */

* Beispiele:

Beispiel mit LTL Formel:

```
//Beschreibung eines Teils der Systemausführung, der nur Zustände mit einer gewünschten Eigenschaft enthält. Dies ist auch bekannt als "Always"

def universality(p):
  G(p)
```

Beispiel mit Schleife und Überladen:

```
//Beschreibung eines Teils der Systemausführung, der eine Instanz bestimmter Ereignisse und Zustände enthält. Dies ist auch bekannt als "Eventually" 
//Mit einem gegebenen n, ist es möglich anzugeben, dass diese Zustände maximal n Mal auftreten.

def existence(p):
  F(p)

def existence(p, n : num):
  var result: G(!p)
  count 0 up to n:
	result: !p W (p W result)
  end
  result
```

Beispiel mit Sequenzaufruf und Überladen:

```
//Beschreibung von Ursache-Wirkungs-Beziehungen zwischen zwei Ereignissen bzw. Zuständen. Auf das Auftreten des ersten Ereignisses (Ursache) muss das Auftreten des zweiten Ereignisses (Wirkung) folgen. Dies ist auch bekannt als Follows und Leads-to.
// Mit einer gegebenen Sequenz von Zuständen, kann man z.B. beschreiben dass eine bestimmte Folge von Zuständen auf einen Zustand folgt

def response(s, p):
  G(p => F(s))
	
def response(s : seq, p):
  G(p => F(seq(s)))
	
def response(s, p : seq):
  G(seq(p) => F(s))
```

[Zurück](Überprüfungen.md)
