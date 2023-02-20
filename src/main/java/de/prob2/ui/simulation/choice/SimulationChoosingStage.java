package de.prob2.ui.simulation.choice;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.google.inject.Singleton;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.SimulationItemHandler;
import de.prob2.ui.simulation.SimulationModel;
import de.prob2.ui.simulation.table.SimulationItem;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Singleton
public class SimulationChoosingStage extends Stage {

	@FXML
	private Button btCheck;

	@FXML
	private HBox timeBox;

	@FXML
	private SimulationMonteCarloChoice simulationMonteCarloChoice;

	@FXML
	private SimulationPropertyChoice simulationPropertyChoice;

	@FXML
	private SimulationHypothesisChoice simulationHypothesisChoice;

	@FXML
	private SimulationEstimationChoice simulationEstimationChoice;

	@FXML
	private VBox inputBox;

	@FXML
	private TextField idTextField;

	private final I18n i18n;

	private final StageManager stageManager;

	private final SimulationItemHandler simulationItemHandler;

	private SimulationModel simulation;

	private SimulationItem lastItem;

	@Inject
	public SimulationChoosingStage(final I18n i18n, final StageManager stageManager, final SimulationItemHandler simulationItemHandler) {
		this.i18n = i18n;
		this.stageManager = stageManager;
		this.simulationItemHandler = simulationItemHandler;
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "simulation_choice.fxml");
	}

	@FXML
	private void initialize() {
		setCheckListeners();
		simulationMonteCarloChoice.checkPropertyProperty().addListener((observable, from, to) -> {
			if(!to) {
				changeGUIType(SimulationType.MONTE_CARLO_SIMULATION);
			} else {
				SimulationType simulationType = simulationPropertyChoice.simulationChoice().getSelectionModel().getSelectedItem();
				changeGUIType(simulationType == null ? SimulationType.MONTE_CARLO_SIMULATION : simulationType);
			}
			this.sizeToScene();
		});
		simulationPropertyChoice.simulationChoice().getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to == null) {
				return;
			}
			changeGUIType(to);
			this.sizeToScene();
		});
		simulationPropertyChoice.simulationChoice().setConverter(i18n.translateConverter());
		simulationHypothesisChoice.visibleProperty().bind(simulationMonteCarloChoice.checkPropertyProperty());
		simulationEstimationChoice.visibleProperty().bind(simulationMonteCarloChoice.checkPropertyProperty());
		simulationPropertyChoice.setChoosingStage(this);
		simulationMonteCarloChoice.setChoosingStage(this);
		this.sizeToScene();
	}

	private void setCheckListeners() {
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
			this.simulationItemHandler.checkItem(existingItem.orElse(newItem));
		});
	}

	private boolean checkSelection() {
		if(!simulationMonteCarloChoice.checkProperty()) {
			return simulationMonteCarloChoice.checkSelection();
		}
		SimulationType type = simulationPropertyChoice.simulationChoice().getSelectionModel().getSelectedItem();
		switch (type) {
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
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		SimulationType type = !simulationMonteCarloChoice.checkProperty() ? SimulationType.MONTE_CARLO_SIMULATION : simulationPropertyChoice.simulationChoice().getSelectionModel().getSelectedItem();
		SimulationItem simulationItem = new SimulationItem(id, type, this.extractInformation());
		simulationItem.setSimulationModel(simulation);
		return simulationItem;
	}

	private Map<String, Object> extractInformation() {
		Map<String, Object> information;
		if(!simulationMonteCarloChoice.checkProperty()) {
			information = simulationMonteCarloChoice.extractInformation();
		} else {
			SimulationType simulationType = simulationPropertyChoice.simulationChoice().getSelectionModel().getSelectedItem();
			information = simulationMonteCarloChoice.extractInformation();
			information.putAll(simulationPropertyChoice.extractInformation());
			switch (simulationType) {
				case ESTIMATION:
					information.putAll(simulationEstimationChoice.extractInformation());
					break;
				case HYPOTHESIS_TEST:
					information.putAll(simulationHypothesisChoice.extractInformation());
					break;
			}
		}
		return information;
	}

	private void changeGUIType(final SimulationType type) {
		inputBox.getChildren().removeAll(timeBox, simulationPropertyChoice, simulationHypothesisChoice, simulationEstimationChoice);
		if(simulationMonteCarloChoice.checkProperty()) {
			inputBox.getChildren().add(0, simulationPropertyChoice);
		}
		switch (type) {
			case MONTE_CARLO_SIMULATION:
				break;
			case ESTIMATION:
				inputBox.getChildren().add(1, simulationEstimationChoice);
				break;
			case HYPOTHESIS_TEST:
				inputBox.getChildren().add(1, simulationHypothesisChoice);
				break;
			default:
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

	public SimulationItem getLastItem() {
		return lastItem;
	}

	public void setSimulation(SimulationModel simulation) {
		this.simulation = simulation;
	}
}
