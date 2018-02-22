package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.chart.HistoryChartStage;
import de.prob2.ui.dotty.DotView;
import de.prob2.ui.formula.FormulaStage;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Heinzen
 * @since 21.09.17
 */
@Singleton
public class VisualisationMenu extends Menu{

	private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationMenu.class);
	
	@FXML
	private MenuItem graphVisualisationItem;

	@FXML
	private MenuItem enterFormulaForVisualization;
	
	private final Injector injector;
	private final CurrentTrace currentTrace;

	@Inject
	public VisualisationMenu(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		stageManager.loadFXML(this, "visualisationMenu.fxml");
	}

	@FXML
	public void initialize() {
		LOGGER.debug("Initializing the visualization-menu!");
		this.enterFormulaForVisualization.disableProperty()
				.bind(currentTrace.currentStateProperty().initializedProperty().not());
		graphVisualisationItem.disableProperty().bind(currentTrace.existsProperty().not());
	}
	
	@FXML
	private void openGraphVisualisation() {
		injector.getInstance(DotView.class).show();
	}

	@FXML
	private void handleFormulaInput() {
		final Stage formulaStage = injector.getInstance(FormulaStage.class);
		formulaStage.showAndWait();
	}

	@FXML
	private void handleHistoryChart() {
		final Stage chartStage = injector.getInstance(HistoryChartStage.class);
		chartStage.show();
		chartStage.toFront();
	}

}
