package de.prob2.ui.simulation;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class SimulationChoosingStage extends Stage {
	@FXML
	private Button btAdd;

	@FXML
	private Button btCheck;

	@FXML
    private HBox timeBox;

	@FXML
	private VBox monteCarloBox;

	@FXML
	private TextField tfTime;

	@FXML
    private TextField tfSimulations;

	@FXML
    private TextField tfSteps;

	@FXML
	private VBox inputBox;

	@FXML
	private ChoiceBox<SimulationChoiceItem> simulationChoice;

	private final ResourceBundle bundle;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final SimulationItemHandler simulationItemHandler;

	private Path path;

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
        inputBox.visibleProperty().bind(simulationChoice.getSelectionModel().selectedItemProperty().isNotNull());
        setCheckListeners();
        simulationChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            if(to == null) {
                return;
            }
            changeGUIType(to.getSimulationType());
            this.sizeToScene();
        });
        this.setOnShown(e -> {

			File configFile = path.toFile();
		});
	}

	private void setCheckListeners() {
        btAdd.setOnAction(e -> {
            this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), this.extractItem());
            this.close();
        });
        btCheck.setOnAction(e -> {
            final SimulationItem newItem = this.extractItem();
            final Optional<SimulationItem> existingItem = this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), newItem);
            this.close();
            this.simulationItemHandler.handleItem(existingItem.orElse(newItem), false);
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
				information.put("TIME", Integer.parseInt(tfTime.getText()));
				break;
			case MODEL_CHECKING:
				break;
			case PROBABILISTIC_MODEL_CHECKING:
				//TODO
				break;
            case MONTE_CARLO_SIMULATION:
                information.put("EXECUTIONS", Integer.parseInt(tfSimulations.getText()));
                information.put("STEPS_PER_EXECUTION", Integer.parseInt(tfSteps.getText()));
				break;
			case TRACE_REPLAY:
				//TODO
				break;
		}
		return information;
	}

    private void changeGUIType(final SimulationType type) {
        inputBox.getChildren().removeAll(timeBox, monteCarloBox);
        switch (type) {
            case TIMING:
                inputBox.getChildren().add(0, timeBox);
                break;
            case MODEL_CHECKING:
                break;
            case PROBABILISTIC_MODEL_CHECKING:
                //TODO
                break;
            case MONTE_CARLO_SIMULATION:
                inputBox.getChildren().add(0, monteCarloBox);
                break;
            case TRACE_REPLAY:
                //TODO
                break;
        }
    }

	@FXML
	public void cancel() {
		this.close();
	}

	public void reset() {
        btAdd.setText(bundle.getString("common.buttons.add"));
        btCheck.setText(bundle.getString("simulation.buttons.addAndCheck"));
        tfTime.clear();
	}

	public void setPath(Path path) {
		this.path = path;
		simulationItemHandler.setPath(path);
	}

}
