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
<?import de.prob2.ui.consoles.b.BConsoleView?>
<?import de.prob2.ui.visualisation.VisualisationsView?>

<fx:root type="MainController" xmlns:fx="http://javafx.com/fxml/1">
	<center>
		<SplitPane fx:id="horizontalSP" dividerPositions="0.3,0.7">
			<SplitPane fx:id="verticalSP" dividerPositions="0.5" orientation="VERTICAL">
				<Accordion fx:id="leftAccordion1" expandedPane="$operationsTP">
					<panes>
						<TitledPane text="%common.views.operations" id="operationsTP" fx:id="operationsTP" collapsible="false">
							<OperationsView/>
						</TitledPane>
					</panes>
				</Accordion>
				<Accordion fx:id="leftAccordion2" expandedPane="$animationTP">
					<panes>
						<TitledPane text="%common.views.animation" id="animationTP" fx:id="animationTP">
							<AnimationView/>
						</TitledPane>
					</panes>
				</Accordion>
			</SplitPane>
			<SplitPane fx:id="verticalSP2" dividerPositions="0.5" orientation="VERTICAL">
				<MainView/>
				<Accordion fx:id="centerAccordion1" expandedPane="$visPane">
					<panes>
						<TitledPane id="visualisations" fx:id="visPane" text="%menu.visualisation">
							<VisualisationsView/>
						</TitledPane>
						<TitledPane id="bconsole" fx:id="consolePane" text="%states.statesView.interactiveConsole.titledPane.title">
							<BConsoleView/>
						</TitledPane>
					</panes>
				</Accordion>
			</SplitPane>
			<SplitPane fx:id="verticalSP3" dividerPositions="0.5" orientation="VERTICAL">
				<Accordion fx:id="rightAccordion1" expandedPane="$statsTP">
					<panes>
						<TitledPane text="%common.views.stats" id="statsTP" fx:id="statsTP">
							<StatsView fx:id="statsView"/>
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
			<fx:reference source="leftAccordion1"/>
			<fx:reference source="leftAccordion2"/>
			<fx:reference source="rightAccordion1"/>
			<fx:reference source="rightAccordion2"/>
			<fx:reference source="centerAccordion1"/>
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
	* VisualisationsView
	* BConsole
* MenuController
* StatusBar

Jede der Komponenten kann so eingesetzt werden, wie man es für richtig hält, aber um sichere Benutzbarkeit zu garantieren, müssen folgende Regeln beachtet werden: Jede entkoppelbare Komponente muss in einer TitledPane enthalten sein und jede dieser TitledPanes muss in ein Accordion gesteckt werden und jedes dieser Accordions muss in einer Liste registriert werden, wie sie am Ende der oben gezeigten fxml-Datei zu sehen ist.

