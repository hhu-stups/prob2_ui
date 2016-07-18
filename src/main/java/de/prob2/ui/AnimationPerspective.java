package de.prob2.ui;

import com.google.inject.Inject;
import de.prob.scripting.Api;
import de.prob2.ui.dotty.DottyView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;

public class AnimationPerspective {

	@SuppressWarnings("unused")
	private final Api api;

	@SuppressWarnings("unused")
	private FXMLLoader loader;

	@FXML
	private Accordion small;

	private OperationsView opsController;
	private HistoryView historyController;
	private DottyView dottyController;
	private ModelcheckingController modelCheckStatsController;

	@Inject
	public AnimationPerspective(
		Api api,
		FXMLLoader loader,
		OperationsView opsController,
		HistoryView historyController,
		DottyView dottyController,
		ModelcheckingController modelCheckStatsController
	) {
		this.api = api;
		this.loader = loader;
		this.opsController = opsController;
		this.historyController = historyController;
		this.dottyController = dottyController;
		this.modelCheckStatsController = modelCheckStatsController;
	}

	@FXML
	public void initialize() {
		small.getPanes().clear();
		small.getPanes().addAll(opsController, historyController, dottyController, modelCheckStatsController);
	}
}
