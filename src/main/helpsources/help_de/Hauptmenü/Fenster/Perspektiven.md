Das Fenster-Submenü erlaubt es Perspektiven zu ändern sowie eigenen Perspektiven zu nutzen. Der FXML-Quellcode für die Standardperspektive ist [im ProB2-UI-Git-Repository](https://github.com/hhu-stups/prob2_ui/blob/develop/src/main/resources/de/prob2/ui/main.fxml) zu finden. Diese FXML-Datei kann kopiert, abgeändert und dann als eigene Perspektive geladen werden.

Jede der Komponenten kann so eingesetzt werden, wie man es für richtig hält, aber um sichere Benutzbarkeit zu garantieren, müssen folgende Regeln beachtet werden: Jede entkoppelbare Komponente muss in einer TitledPane enthalten sein und jede dieser TitledPanes muss in ein Accordion gesteckt werden und jedes dieser Accordions muss in einer Liste registriert werden, wie sie am Ende der oben verlinkten FXML-Datei zu sehen ist.

