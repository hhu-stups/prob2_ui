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
import de.prob2.ui.simulation.SimulationMode;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.table.SimulationItem;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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

	private final SimulationMode simulationMode;

	private SimulationModel simulation;

	private SimulationItem lastItem;

	@Inject
	public SimulationChoosingStage(final I18n i18n, final StageManager stageManager, final SimulationItemHandler simulationItemHandler, final SimulationMode simulationMode) {
		this.i18n = i18n;
		this.stageManager = stageManager;
		this.simulationItemHandler = simulationItemHandler;
		this.simulationMode = simulationMode;
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "simulation_choice.fxml");
	}

	@FXML
	private void initialize() {
		setCheckListeners();
		simulationMonteCarloChoice.checkPropertyProperty().addListener((observable, from, to) -> updateGUI());
		simulationPropertyChoice.simulationChoice().getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> updateGUI());
		simulationPropertyChoice.simulationChoice().setConverter(i18n.translateConverter());
		simulationPropertyChoice.setChoosingStage(this);
		simulationMonteCarloChoice.setChoosingStage(this);
	}

	private void updateGUI() {
		changeGUIType();
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
		if(simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO && !simulationMonteCarloChoice.checkProperty()) {
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
		SimulationType type = null;
		if(simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO && !simulationMonteCarloChoice.checkProperty()) {
			type = SimulationType.MONTE_CARLO_SIMULATION;
		} else {
			type = simulationPropertyChoice.simulationChoice().getSelectionModel().getSelectedItem();
		}
		SimulationItem simulationItem = new SimulationItem(id, type, this.extractInformation());
		simulationItem.setSimulationModel(simulation);
		return simulationItem;
	}

	private Map<String, Object> extractInformation() {
		Map<String, Object> information = new HashMap<>();
		if(simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO) {
			information.putAll(simulationMonteCarloChoice.extractInformation());
		}
		if(simulationMode.getMode() == SimulationMode.Mode.BLACK_BOX || simulationMonteCarloChoice.checkProperty()) {
			SimulationType simulationType = simulationPropertyChoice.simulationChoice().getSelectionModel().getSelectedItem();
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

	private void changeGUIType() {
		inputBox.getChildren().removeAll(simulationMonteCarloChoice, simulationPropertyChoice, simulationHypothesisChoice, simulationEstimationChoice);
		SimulationMode.Mode mode = simulationMode.getMode();

		SimulationType type = simulationPropertyChoice.simulationChoice().getSelectionModel().getSelectedItem();
		if(type != null) {
			if(type == SimulationType.ESTIMATION) {
				inputBox.getChildren().add(0, simulationEstimationChoice);
			} else if(type == SimulationType.HYPOTHESIS_TEST) {
				inputBox.getChildren().add(0, simulationHypothesisChoice);
			}
		}

		if(simulationMonteCarloChoice.checkProperty() || mode == SimulationMode.Mode.BLACK_BOX) {
			inputBox.getChildren().add(0, simulationPropertyChoice);
		}

		inputBox.getChildren().add(0, simulationMonteCarloChoice);

		// Change order so that validation task id is always at the bottom
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
		updateGUI();
	}
}
