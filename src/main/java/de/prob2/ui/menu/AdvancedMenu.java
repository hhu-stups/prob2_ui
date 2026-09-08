package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.tracereplay.interactive.InteractiveTraceReplayView;
import de.prob2.ui.animation.tracereplay.refactoring.RefactorSetupView;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.dataimport.CSVDataImportDialog;
import de.prob2.ui.dataimport.JSONDataImportDialog;
import de.prob2.ui.dataimport.RailML2BDataImportDialog;
import de.prob2.ui.dataimport.XML2BDataImportDialog;
import de.prob2.ui.dataimport.XMLDataImportDialog;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.output.PrologOutputStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.vomanager.VOManagerStage;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public final class AdvancedMenu extends Menu {
	@FXML
	private MenuItem interactiveTraceItem;

	@FXML
	private MenuItem refactorTraceItem;

	@FXML
	private Menu importDataFiles;

	private final StageManager stageManager;
	private final Injector injector;

	@Inject
	public AdvancedMenu(
		StageManager stageManager,
		CurrentProject currentProject,
		CurrentTrace currentTrace,
		Injector injector
	) {
		this.stageManager = stageManager;
		this.injector = injector;
		stageManager.loadFXML(this, "advancedMenu.fxml");
		interactiveTraceItem.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(currentTrace.animatorBusyProperty()));
		refactorTraceItem.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(currentTrace.animatorBusyProperty()));
	}

	@FXML
	private void handleGroovyConsole() {
		final Stage groovyConsoleStage = injector.getInstance(GroovyConsoleStage.class);
		groovyConsoleStage.show();
		groovyConsoleStage.toFront();
	}

	@FXML
	private void handlePrologOutput() {
		final Stage prologOutputStage = injector.getInstance(PrologOutputStage.class);
		prologOutputStage.show();
		prologOutputStage.toFront();
	}

	@FXML
	private void openSimulator() {
		SimulatorStage simulatorStage = injector.getInstance(SimulatorStage.class);
		simulatorStage.show();
		simulatorStage.toFront();
	}

	@FXML
	private void showInteractiveTraceReplay() {
		InteractiveTraceReplayView iReplayStage = injector.getInstance(InteractiveTraceReplayView.class);
		iReplayStage.addToMainView();
		iReplayStage.toFront();
	}

	@FXML
	private void showRefactorTrace() {
		injector.getInstance(RefactorSetupView.class).showAndPerformAction();
	}

	@FXML
	private void showXML2BImport() {
		injector.getInstance(XML2BDataImportDialog.class).showAndWait();
	}

	@FXML
	private void showXMLImport() {
		injector.getInstance(XMLDataImportDialog.class).showAndWait();
	}

	@FXML
	private void showJSONImport() {
		injector.getInstance(JSONDataImportDialog.class).showAndWait();
	}

	@FXML
	private void showCSVImport() {
		injector.getInstance(CSVDataImportDialog.class).showAndWait();
	}

	@FXML
	private void showRailMLImport() {
		RailML2BDataImportDialog dialog = injector.getInstance(RailML2BDataImportDialog.class);
		dialog.show();
		dialog.toFront();
	}

	@FXML
	private void openVOManager() {
		VOManagerStage voManagerStage = injector.getInstance(VOManagerStage.class);
		voManagerStage.show();
		voManagerStage.toFront();

		Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "", "menu.advanced.items.vomanager.warningMessage");
		alert.initOwner(voManagerStage);
		alert.initModality(Modality.WINDOW_MODAL);
		alert.show();
	}

}
