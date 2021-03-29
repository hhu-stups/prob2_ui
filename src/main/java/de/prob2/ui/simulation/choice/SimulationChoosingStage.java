package de.prob2.ui.simulation.choice;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

	public static class SimulationChoiceItem {

		private final SimulationType simulationType;

		public SimulationChoiceItem(@NamedArg("simulationType") SimulationType simulationType) {
			this.simulationType = simulationType;
		}

		@Override
		public String toString() {
			return simulationType.getName();
		}

		public SimulationType getSimulationType() {
			return simulationType;
		}

	}

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
	private VBox inputBox;

	@FXML
	private ChoiceBox<SimulationChoiceItem> simulationChoice;

	private final StageManager stageManager;

	private final ResourceBundle bundle;

	private final CurrentProject currentProject;

	private final SimulationItemHandler simulationItemHandler;

	@Inject
	public SimulationChoosingStage(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject,
								   final SimulationItemHandler simulationItemHandler) {
		this.stageManager = stageManager;
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
	}

	private void setCheckListeners() {
        btAdd.setOnAction(e -> {
			boolean validChoice = checkSelection();
			if(!validChoice) {
				showInvalidSelection();
				return;
			}
            this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), this.extractItem());
            this.close();
        });
        btCheck.setOnAction(e -> {
        	boolean validChoice = checkSelection();
			if(!validChoice) {
				showInvalidSelection();
				return;
			}
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
			default:
				break;
		}
		return true;
	}

	private void showInvalidSelection() {
		final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, "simulation.error.header.invalid", "simulation.error.body.invalid");
		alert.initOwner(this);
		alert.showAndWait();
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
		}
		return information;
	}

    private void changeGUIType(final SimulationType type) {
        inputBox.getChildren().removeAll(timeBox, simulationMonteCarloChoice, simulationHypothesisChoice, simulationEstimationChoice);
		simulationHypothesisChoice.clear();
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
	}

	public void setPath(Path path) {
		simulationItemHandler.setPath(path);
	}

}
