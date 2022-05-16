package de.prob2.ui.simulation.choice;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.SimulationModel;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.beans.NamedArg;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
	private VBox inputBox;

	@FXML
	private ChoiceBox<SimulationType> simulationChoice;
	private final ResourceBundle bundle;

	private final StageManager stageManager;

	private final SimulationItemHandler simulationItemHandler;

	private final SimulationChoiceBindings simulationChoiceBindings;

	private SimulationModel simulation;

	private SimulationItem lastItem;

	@Inject
	public SimulationChoosingStage(final ResourceBundle bundle, final StageManager stageManager, final SimulationItemHandler simulationItemHandler, final SimulationChoiceBindings simulationChoiceBindings) {
		this.bundle = bundle;
		this.stageManager = stageManager;
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
			changeGUIType(to);
			this.sizeToScene();
		});
		simulationChoice.setConverter(new StringConverter<SimulationType>() {
			@Override
			public String toString(final SimulationType object) {
				if(object == null) {
					return "";
				}
				return bundle.getString(object.getTranslationKey());
			}

			@Override
			public SimulationType fromString(final String string) {
				throw new UnsupportedOperationException("Conversion from String to SimulationType not supported");
			}
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
			this.simulationItemHandler.addItem(simulation, lastItem);
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
			final Optional<SimulationItem> existingItem = this.simulationItemHandler.addItem(simulation, newItem);
			lastItem = existingItem.orElse(newItem);
			this.close();
			this.simulationItemHandler.checkItem(existingItem.orElse(newItem), false);
		});
	}

	private boolean checkSelection() {
		SimulationType type = simulationChoice.getSelectionModel().getSelectedItem();
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
		SimulationItem simulationItem = new SimulationItem(simulationChoice.getSelectionModel().getSelectedItem(), this.extractInformation());
		simulationItem.setSimulationModel(simulation);
		return simulationItem;
	}

	private Map<String, Object> extractInformation() {
		SimulationType simulationType = simulationChoice.getSelectionModel().getSelectedItem();
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

		simulationMonteCarloChoice.bindMaxStepsBeforePropertyProperty(simulationChoiceBindings.maxStepsBeforePropertyProperty());
		simulationHypothesisChoice.bindMaxStepsBeforePropertyProperty(simulationChoiceBindings.maxStepsBeforePropertyProperty());
		simulationEstimationChoice.bindMaxStepsBeforePropertyProperty(simulationChoiceBindings.maxStepsBeforePropertyProperty());

		simulationMonteCarloChoice.bindStartingProperty(simulationChoiceBindings.startAfterProperty(), simulationChoiceBindings.startingPredicateProperty(), simulationChoiceBindings.startingTimeProperty());
		simulationHypothesisChoice.bindStartingProperty(simulationChoiceBindings.startAfterProperty(), simulationChoiceBindings.startingPredicateProperty(), simulationChoiceBindings.startingTimeProperty());
		simulationEstimationChoice.bindStartingProperty(simulationChoiceBindings.startAfterProperty(), simulationChoiceBindings.startingPredicateProperty(), simulationChoiceBindings.startingTimeProperty());

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

	public void setSimulation(SimulationModel simulation) {
		this.simulation = simulation;
	}
}
