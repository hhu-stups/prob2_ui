package de.prob2.ui.simulation.choice;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.SimulationItemHandler;
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

	private final CurrentProject currentProject;

	private final SimulationItemHandler simulationItemHandler;

	private final SimulationChoiceBindings simulationChoiceBindings;

	private SimulationItem lastItem;

	@Inject
	public SimulationChoosingStage(final StageManager stageManager, final CurrentProject currentProject,
								   final SimulationItemHandler simulationItemHandler, final SimulationChoiceBindings simulationChoiceBindings) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.simulationItemHandler = simulationItemHandler;
		this.simulationChoiceBindings = simulationChoiceBindings;
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
		bindCommonProperties();
	}

	private void setCheckListeners() {
		btAdd.setOnAction(e -> {
			lastItem = null;
			boolean validChoice = checkSelection();
			if(!validChoice) {
				showInvalidSelection();
				return;
			}
			lastItem = this.extractItem();
			this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), lastItem);
			this.close();
		});
		btCheck.setOnAction(e -> {
			lastItem = null;
			boolean validChoice = checkSelection();
			if(!validChoice) {
				showInvalidSelection();
				return;
			}
			final SimulationItem newItem = this.extractItem();
			final Optional<SimulationItem> existingItem = this.simulationItemHandler.addItem(currentProject.getCurrentMachine(), newItem);
			lastItem = existingItem.orElse(newItem);
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

	public void setPath(Path path) {
		simulationItemHandler.setPath(path);
	}

	private void bindCommonProperties() {
		// These bindings are used to synchronize the labels in SimulationMonteCarloChoice, SimulationHypothesisChoice, and SimulationEstimationChoice
		simulationMonteCarloChoice.bindSimulationsProperty(simulationChoiceBindings.simulationsProperty());
		simulationHypothesisChoice.bindSimulationsProperty(simulationChoiceBindings.simulationsProperty());
		simulationEstimationChoice.bindSimulationsProperty(simulationChoiceBindings.simulationsProperty());

		simulationMonteCarloChoice.bindInitialProperty(simulationChoiceBindings.initialStepsProperty(), simulationChoiceBindings.initialPredicateProperty(), simulationChoiceBindings.initialTimeProperty());
		simulationHypothesisChoice.bindInitialProperty(simulationChoiceBindings.initialStepsProperty(), simulationChoiceBindings.initialPredicateProperty(), simulationChoiceBindings.initialTimeProperty());
		simulationEstimationChoice.bindInitialProperty(simulationChoiceBindings.initialStepsProperty(), simulationChoiceBindings.initialPredicateProperty(), simulationChoiceBindings.initialTimeProperty());

		simulationMonteCarloChoice.bindInitialItemProperty(simulationChoiceBindings.initialItemProperty());
		simulationHypothesisChoice.bindInitialItemProperty(simulationChoiceBindings.initialItemProperty());
		simulationEstimationChoice.bindInitialItemProperty(simulationChoiceBindings.initialItemProperty());

		simulationMonteCarloChoice.bindStartingProperty(simulationChoiceBindings.startAfterProperty(), simulationChoiceBindings.startingPredicateProperty(), simulationChoiceBindings.initialTimeProperty());
		simulationHypothesisChoice.bindStartingProperty(simulationChoiceBindings.startAfterProperty(), simulationChoiceBindings.startingPredicateProperty(), simulationChoiceBindings.initialTimeProperty());
		simulationEstimationChoice.bindStartingProperty(simulationChoiceBindings.startAfterProperty(), simulationChoiceBindings.startingPredicateProperty(), simulationChoiceBindings.initialTimeProperty());

		simulationMonteCarloChoice.bindStartingItemProperty(simulationChoiceBindings.startingItemProperty());
		simulationHypothesisChoice.bindStartingItemProperty(simulationChoiceBindings.startingItemProperty());
		simulationEstimationChoice.bindStartingItemProperty(simulationChoiceBindings.startingItemProperty());

		simulationMonteCarloChoice.bindEndingProperty(simulationChoiceBindings.stepsProperty(), simulationChoiceBindings.endingPredicateProperty(), simulationChoiceBindings.endingTimeProperty());
		simulationHypothesisChoice.bindEndingProperty(simulationChoiceBindings.stepsProperty(), simulationChoiceBindings.endingPredicateProperty(), simulationChoiceBindings.endingTimeProperty());
		simulationEstimationChoice.bindEndingProperty(simulationChoiceBindings.stepsProperty(), simulationChoiceBindings.endingPredicateProperty(), simulationChoiceBindings.endingTimeProperty());

		simulationMonteCarloChoice.bindEndingItemProperty(simulationChoiceBindings.endingItemProperty());
		simulationHypothesisChoice.bindEndingItemProperty(simulationChoiceBindings.endingItemProperty());
		simulationEstimationChoice.bindEndingItemProperty(simulationChoiceBindings.endingItemProperty());

		simulationHypothesisChoice.bindMonteCarloTimeProperty(simulationChoiceBindings.monteCarloTimeProperty());
		simulationEstimationChoice.bindMonteCarloTimeProperty(simulationChoiceBindings.monteCarloTimeProperty());

		simulationHypothesisChoice.bindPredicateProperty(simulationChoiceBindings.predicateProperty());
		simulationEstimationChoice.bindPredicateProperty(simulationChoiceBindings.predicateProperty());

		simulationHypothesisChoice.bindCheckingProperty(simulationChoiceBindings.checkingProperty());
		simulationEstimationChoice.bindCheckingProperty(simulationChoiceBindings.checkingProperty());
	}

	public SimulationItem getLastItem() {
		return lastItem;
	}
}
