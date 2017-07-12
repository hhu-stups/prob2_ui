package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.chart.HistoryChartStage;
import de.prob2.ui.formula.FormulaInputDialog;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class FormulaMenu extends Menu {

	@FXML
	private MenuItem enterFormulaForVisualization;

	private final CurrentTrace currentTrace;
	private final Injector injector;

	@Inject
	private FormulaMenu(final StageManager stageManager, final CurrentTrace currentTrace, final Injector injector) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		stageManager.loadFXML(this, "formulaMenu.fxml");
	}

	@FXML
	public void initialize() {
		this.enterFormulaForVisualization.disableProperty()
				.bind(currentTrace.currentStateProperty().initializedProperty().not());
	}

	@FXML
	private void handleFormulaInput() {
		final Dialog<Void> formulaInputStage = injector.getInstance(FormulaInputDialog.class);
		formulaInputStage.showAndWait();
	}

	@FXML
	private void handleHistoryChart() {
		final Stage chartStage = injector.getInstance(HistoryChartStage.class);
		chartStage.show();
		chartStage.toFront();
	}
}
