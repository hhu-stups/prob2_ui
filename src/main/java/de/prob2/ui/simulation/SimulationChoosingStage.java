package de.prob2.ui.simulation;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;
import java.util.ResourceBundle;

public class SimulationChoosingStage extends Stage {
	@FXML
	private Button btAdd;

	@FXML
	private Button btCheck;

	@FXML
	private TextField tfTime;

	@FXML
	private VBox formulaInput;

	@FXML
	private ChoiceBox<SimulationChoiceItem> cbChoice;

	private final ResourceBundle bundle;

	protected final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	@Inject
	public SimulationChoosingStage(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject, final CurrentTrace currentTrace) {
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;

		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "simulation_choice.fxml");
	}

	@FXML
	public void cancel() {
		this.close();
	}

	public void reset() {

	}
}
