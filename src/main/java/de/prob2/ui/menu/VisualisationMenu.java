package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.chart.HistoryChartStage;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.dynamic.table.ExpressionTableView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.visb.VisBStage;
import de.prob2.ui.visualisation.magiclayout.MagicLayoutView;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public class VisualisationMenu extends Menu {
	@FXML
	private MenuItem graphVisualization;
	@FXML
	private MenuItem tableVisualization;

	private final Injector injector;
	private final CurrentProject currentProject;

	@Inject
	public VisualisationMenu(final StageManager stageManager, final Injector injector,
			final CurrentProject currentProject) {
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "visualisationMenu.fxml");
	}

	@FXML
	public void initialize() {
		this.graphVisualization.disableProperty().bind(currentProject.currentMachineProperty().isNull());
		this.tableVisualization.disableProperty().bind(currentProject.currentMachineProperty().isNull());
	}

	@FXML
	private void openGraphVisualisation() {
		DotView dotView = injector.getInstance(DotView.class);
		dotView.show();
		dotView.toFront();
	}

	@FXML
	private void openTableVisualisation() {
		ExpressionTableView expressionTableView = injector.getInstance(ExpressionTableView.class);
		expressionTableView.show();
		expressionTableView.toFront();
	}
	
	@FXML
	private void openMagicLayout() {
		MagicLayoutView magicLayout = injector.getInstance(MagicLayoutView.class);
		magicLayout.show();
		magicLayout.toFront();
	}
	
	@FXML
	void openVisB(){
		final Stage visBStage = injector.getInstance(VisBStage.class);
		visBStage.show();
		visBStage.toFront();
	}

	@FXML
	private void handleHistoryChart() {
		final Stage chartStage = injector.getInstance(HistoryChartStage.class);
		chartStage.show();
		chartStage.toFront();
	}

}
