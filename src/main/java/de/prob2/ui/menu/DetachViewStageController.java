package de.prob2.ui.menu;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;

import de.prob2.ui.MainController;
import de.prob2.ui.animation.AnimationView;
import de.prob2.ui.consoles.b.BConsoleView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.VerificationsView;
import de.prob2.ui.visualisation.VisualisationsView;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class DetachViewStageController extends Stage {
	@FXML private Button apply;
	@FXML private Button attachAll;
	@FXML private CheckBox detachOperations;
	@FXML private CheckBox detachAnimation;
	@FXML private CheckBox detachHistory;
	@FXML private CheckBox detachVerifications;
	@FXML private CheckBox detachStats;
	@FXML private CheckBox detachProject;
	@FXML private CheckBox detachConsole;
	@FXML private CheckBox detachVisualisations;

	private final MainController mainController;
	
	private final Map<Class<?>, CheckBox> checkBoxMap;
	
	@Inject
	private DetachViewStageController(StageManager stageManager, MainController mainController) {
		this.mainController = mainController;
		
		checkBoxMap = new HashMap<>();
		
		stageManager.loadFXML(this, "detachedPerspectivesChoice.fxml", null);
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		checkBoxMap.put(OperationsView.class, detachOperations);
		checkBoxMap.put(AnimationView.class, detachAnimation);
		checkBoxMap.put(HistoryView.class, detachHistory);
		checkBoxMap.put(VerificationsView.class, detachVerifications);
		checkBoxMap.put(StatsView.class, detachStats);
		checkBoxMap.put(ProjectView.class, detachProject);
		checkBoxMap.put(BConsoleView.class, detachConsole);
		checkBoxMap.put(VisualisationsView.class, detachVisualisations);

		for (CheckBox checkBox : checkBoxMap.values()) {
			checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> apply.setDisable(false));
		}
		this.setOnShowing(e -> {
			var detachedViews = mainController.getDetachedViews();
			checkBoxMap.forEach((clazz, checkBox) -> checkBox.setSelected(detachedViews.contains(clazz)));
			attachAll.setDisable(detachedViews.isEmpty());
			apply.setDisable(true);
		});
	}

	@FXML
	private void apply() {
		checkBoxMap.forEach((clazz, checkBox) -> {
			if (checkBox.isSelected()) {
				mainController.detachView(clazz);
			} else {
				mainController.attachView(clazz);
			}
		});
		this.hide();
	}

	@FXML
	private void doAttachAll() {
		mainController.attachAllViews();
		this.hide();
	}
}
