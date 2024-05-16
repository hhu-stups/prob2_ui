package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.chart.HistoryChartStage;
import de.prob2.ui.dynamic.DynamicVisualizationStage;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.dynamic.plantuml.PlantUmlView;
import de.prob2.ui.dynamic.table.ExpressionTableView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.magiclayout.MagicLayoutView;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public class VisualisationMenu extends Menu {
	private final Injector injector;

	@Inject
	public VisualisationMenu(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "visualisationMenu.fxml");
	}

	@FXML
	private void openDynamicVisualisation() {
		DynamicVisualizationStage stage = injector.getInstance(DynamicVisualizationStage.class);
		stage.show();
		stage.toFront();
	}

	@FXML
	private void openGraphVisualisation() {
		DotView dotView = injector.getInstance(DotView.class);
		dotView.show();
		dotView.toFront();
	}

	@FXML
	private void openPlantUmlVisualisation() {
		PlantUmlView pumlView = injector.getInstance(PlantUmlView.class);
		pumlView.show();
		pumlView.toFront();
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
	private void handleHistoryChart() {
		final Stage chartStage = injector.getInstance(HistoryChartStage.class);
		chartStage.show();
		chartStage.toFront();
	}

}
