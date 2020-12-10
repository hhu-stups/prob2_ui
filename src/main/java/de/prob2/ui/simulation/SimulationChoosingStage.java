package de.prob2.ui.simulation;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
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
	private ChoiceBox<SimulationChoiceItem> simulationChoice;

	private final ResourceBundle bundle;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final SimulationItemHandler simulationItemHandler;

	@Inject
	public SimulationChoosingStage(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject, final CurrentTrace currentTrace,
								   final SimulationItemHandler simulationItemHandler) {
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.simulationItemHandler = simulationItemHandler;
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "simulation_choice.fxml");
	}

	@FXML
	private void initialize() {
		btAdd.setOnAction(e -> {
			this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), this.extractItem());
			this.close();
		});
	}

	private SimulationItem extractItem() {
		return new SimulationItem(new SimulationCheckingConfiguration(this.extractType(), this.extractInformation()), "");
	}

	private SimulationType extractType() {
		SimulationChoiceItem item = simulationChoice.getSelectionModel().getSelectedItem();
		return item.getSimulationType();
	}

	private Map<String, Object> extractInformation() {
		SimulationChoiceItem item = simulationChoice.getSelectionModel().getSelectedItem();
		SimulationType simulationType = item.getSimulationType();
		Map<String, Object> information = new HashMap<>();
		switch (simulationType) {
			case TIMING:
				information.put("time", Integer.parseInt(tfTime.getText()));
				break;
			case MODEL_CHECKING:
				break;
			case PROBABILISTIC_MODEL_CHECKING:
				//TODO
				break;
			case HYPOTHESIS_TEST:
				//TODO
				break;
			case TRACE_REPLAY:
				//TODO
				break;
		}
		return information;
	}

	@FXML
	public void cancel() {
		this.close();
	}

	public void reset() {

	}
}
