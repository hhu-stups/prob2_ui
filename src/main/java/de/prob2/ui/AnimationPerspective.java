package de.prob2.ui;

import com.google.inject.Inject;
import de.prob2.ui.dotty.DottyView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

public class AnimationPerspective {

	@FXML
	private Accordion small;

	private OperationsView opsController;
	private HistoryView historyController;
	private DottyView dottyController;
	private ModelcheckingController modelCheckStatsController;

	@Inject
	public AnimationPerspective(
		OperationsView opsController,
		HistoryView historyController,
		DottyView dottyController,
		ModelcheckingController modelCheckStatsController
	) {
		this.opsController = opsController;
		this.historyController = historyController;
		this.dottyController = dottyController;
		this.modelCheckStatsController = modelCheckStatsController;
	}

	@FXML
	public void initialize() {
		small.getPanes().clear();
		small.getPanes().addAll(
				new TitledPane("Operations",opsController),
				new TitledPane("History",historyController),
				new TitledPane("Dotty",dottyController),
				new TitledPane("Model Check",modelCheckStatsController));
	}
}
