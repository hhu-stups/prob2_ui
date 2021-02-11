package de.prob2.ui.simulation.choice;

import com.google.inject.Injector;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;
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
	private SimulationMonteCarloChoice simulationMonteCarloChoice;

	@FXML
	private SimulationHypothesisChoice simulationHypothesisChoice;

	@FXML
	private SimulationEstimationChoice simulationEstimationChoice;

	@FXML
	private SimulationTraceChoice tracesChoice;

	@FXML
	private VBox inputBox;

	@FXML
	private ChoiceBox<SimulationChoiceItem> simulationChoice;

	private final Injector injector;

	private final ResourceBundle bundle;

	private final CurrentProject currentProject;

	private final SimulationItemHandler simulationItemHandler;

	private Path path;

	@Inject
	public SimulationChoosingStage(final StageManager stageManager, final Injector injector, final ResourceBundle bundle, final CurrentProject currentProject,
								   final SimulationItemHandler simulationItemHandler) {
		this.injector = injector;
		this.bundle = bundle;
		this.currentProject = currentProject;
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
		simulationMonteCarloChoice.setSimulationChoosingStage(this);
		simulationHypothesisChoice.setSimulationChoosingStage(this);
		simulationEstimationChoice.setSimulationChoosingStage(this);
		tracesChoice.setSimulationChoosingStage(this);
	}

	private void setCheckListeners() {
        btAdd.setOnAction(e -> {
			boolean validChoice = checkSelection();
			if(!validChoice) {
				return;
			}
			// TODO: Inform user
            this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), this.extractItem());
            this.close();
        });
        btCheck.setOnAction(e -> {
        	boolean validChoice = checkSelection();
        	if(!validChoice) {
        		return;
			}
        	// TODO: Inform user
            final SimulationItem newItem = this.extractItem();
            final Optional<SimulationItem> existingItem = this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), newItem);
            this.close();
            this.simulationItemHandler.checkItem(existingItem.orElse(newItem), false);
        });
    }

    private boolean checkSelection() {
		SimulationChoiceItem item = simulationChoice.getSelectionModel().getSelectedItem();
		SimulationType type = item.getSimulationType();
		switch (type) {
			case MONTE_CARLO_SIMULATION:
				return simulationMonteCarloChoice.checkSelection();
			case ESTIMATION:
				return simulationEstimationChoice.checkSelection();
			case HYPOTHESIS_TEST:
				return simulationHypothesisChoice.checkSelection();
			case TRACE_REPLAY:
				return tracesChoice.checkSelection();
			default:
				break;
		}
		return true;
	}


	private SimulationItem extractItem() {
		return new SimulationItem(this.extractType(), this.extractInformation());
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
			case MONTE_CARLO_SIMULATION:
				information = simulationMonteCarloChoice.extractInformation();
				break;
			case ESTIMATION:
				information = simulationEstimationChoice.extractInformation();
				break;
			case HYPOTHESIS_TEST:
				information = simulationHypothesisChoice.extractInformation();
				break;
			case TRACE_REPLAY:
				information = tracesChoice.extractInformation();
				break;
		}
		return information;
	}

    private void changeGUIType(final SimulationType type) {
        inputBox.getChildren().removeAll(timeBox, simulationMonteCarloChoice, simulationHypothesisChoice, simulationEstimationChoice, tracesChoice);
		simulationHypothesisChoice.clear();
		tracesChoice.clear();
        switch (type) {
			case MONTE_CARLO_SIMULATION:
				inputBox.getChildren().add(0, simulationMonteCarloChoice);
				break;
			case ESTIMATION:
				inputBox.getChildren().add(0, simulationEstimationChoice);
				break;
			case HYPOTHESIS_TEST:
                inputBox.getChildren().add(0, simulationHypothesisChoice);
                break;
            case TRACE_REPLAY:
            	tracesChoice.updateTraces(injector.getInstance(TraceViewHandler.class).getTraces());
            	inputBox.getChildren().add(0, tracesChoice);
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
        simulationMonteCarloChoice.clear();
		simulationHypothesisChoice.clear();
		simulationEstimationChoice.clear();
        tracesChoice.clear();
	}

	public void setPath(Path path) {
		this.path = path;
		simulationItemHandler.setPath(path);
	}

}
