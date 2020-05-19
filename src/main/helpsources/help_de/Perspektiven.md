# Eigene Perspektiven erstellen

Das Fenster-Submenü erlaubt es Perspektiven zu ändern sowie eigenen Perspektiven zu nutzen. Dies ist der ganze FXML-Code, der in der voreingestellten Perspektive genutzt wird.

```XML
<?xml version="1.0" encoding="UTF-8"?>

<?import de.prob2.ui.history.HistoryView?>
<?import de.prob2.ui.MainController?>
<?import de.prob2.ui.menu.MainView?>
<?import de.prob2.ui.menu.MenuController?>
<?import de.prob2.ui.operations.OperationsView?>
<?import de.prob2.ui.project.ProjectView?>
<?import de.prob2.ui.stats.StatsView?>
<?import de.prob2.ui.statusbar.StatusBar?>
<?import de.prob2.ui.verifications.VerificationsView?>
<?import de.prob2.ui.animation.AnimationView?>
<?import javafx.collections.FXCollections?>
<?import javafx.scene.control.Accordion?>
<?import javafx.scene.control.SplitPane?>
<?import javafx.scene.control.TitledPane?>

<fx:root type="MainController" maxHeight="Infinity"
		 maxWidth="Infinity" minHeight="700.0" minWidth="1000.0"
		 prefHeight="400.0" prefWidth="600.0" xmlns:fx="http://javafx.com/fxml/1">
	<center>
		<SplitPane fx:id="horizontalSP" dividerPositions="0.1,0.9">
			<Accordion fx:id="leftAccordion" expandedPane="$operationsTP">
				<panes>
					<TitledPane text="%common.views.operations" id="operationsTP" fx:id="operationsTP">
						<OperationsView/>
					</TitledPane>
					<TitledPane text="%common.views.animation" id="animationTP" fx:id="animationTP">
						<AnimationView/>
					</TitledPane>
				</panes>
			</Accordion>
			<SplitPane fx:id="verticalSP" dividerPositions="0.9" orientation="VERTICAL">
				<MainView/>
			</SplitPane>
			<SplitPane fx:id="verticalSP2" dividerPositions="0.5" orientation="VERTICAL">
				<Accordion fx:id="rightAccordion1" expandedPane="$statsTP">
					<panes>
						<TitledPane text="%common.views.stats" id="statsTP" fx:id="statsTP">
							<StatsView/>
						</TitledPane>
						<TitledPane text="%common.views.verifications" id="verificationsTP" fx:id="verificationsTP">
							<VerificationsView/>
						</TitledPane>
						<TitledPane text="%common.views.project" id="projectTP" fx:id="projectTP">
							<ProjectView fx:id="projectView"/>
						</TitledPane>
					</panes>
				</Accordion>
				<Accordion fx:id="rightAccordion2" expandedPane="$historyTP">
					<panes>
						<!-- Note: The title text of historyTP is changed in MainController.initialize to include the history size. -->
						<TitledPane text="%common.views.history" id="historyTP" fx:id="historyTP" collapsible="false">
							<HistoryView fx:id="historyView"/>
						</TitledPane>
					</panes>
				</Accordion>
			</SplitPane>
		</SplitPane>
	</center>
	<top>
		<MenuController/>
	</top>
	<bottom>
		<StatusBar/>
	</bottom>
	<fx:define>
		<FXCollections fx:id="accordions" fx:factory="observableArrayList">
			<fx:reference source="leftAccordion"/>
			<fx:reference source="rightAccordion1"/>
			<fx:reference source="rightAccordion2"/>
		</FXCollections>
	</fx:define>
</fx:root>
```
Wie man sehen kann, besteht die voreingestellte Perspektive aus diversen Komponenten:

* MainController
* MainView
* Entkoppelbare Komponenten:
	* OperationsView
	* AnimationView
	* StatsView
	* VerificationsView
	* ProjectView
	* HistoryView
* MenuController
* StatusBar

Jede der Komponenten kann so eingesetzt werden, wie man es für richtig hält, aber um sichere Benutzbarkeit zu garantieren, müssen folgende Regeln beachtet werden: Jede entkoppelbare Komponente muss in einer TitledPane enthalten sein und jede dieser TitledPanes muss in ein Accordion gesteckt werden und jedes dieser Accordions muss in einer Liste registriert werden.
```XML
...
			<Accordion fx:id="leftAccordion" expandedPane="$operationsTP">
				<panes>
					<TitledPane text="%common.views.operations" id="operationsTP" fx:id="operationsTP">
						<OperationsView/>
					</TitledPane>
					<TitledPane text="%common.views.animation" id="animationTP" fx:id="animationTP">
						<AnimationView/>
					</TitledPane>
				</panes>
			</Accordion>
...
	<fx:define>
		<FXCollections fx:id="accordions" fx:factory="observableArrayList">
			<fx:reference source="leftAccordion"/>
			<fx:reference source="rightAccordion1"/>
			<fx:reference source="rightAccordion2"/>
		</FXCollections>
	</fx:define>
...
``` 
