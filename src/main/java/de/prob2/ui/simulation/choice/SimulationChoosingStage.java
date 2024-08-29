package de.prob2.ui.simulation.choice;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.SimulationMode;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.table.SimulationItem;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Singleton
public final class SimulationChoosingStage extends Stage {
	@FXML
	private Button btApply;

	@FXML
	private SimulationMonteCarloChoice simulationMonteCarloChoice;

	@FXML
	private SimulationConditionChoice simulationConditionChoice;

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

	private final SimulationMode simulationMode;

	private SimulationModel simulation;

	private SimulationItem result;

	@Inject
	private SimulationChoosingStage(I18n i18n, StageManager stageManager, SimulationMode simulationMode) {
		this.i18n = i18n;
		this.stageManager = stageManager;
		this.simulationMode = simulationMode;
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "simulation_choice.fxml");
	}

	@FXML
	private void initialize() {
		setCheckListeners();
		simulationConditionChoice.simulationChoice().getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> updateGUI());
		simulationConditionChoice.simulationChoice().setConverter(i18n.translateConverter());
		simulationPropertyChoice.setChoosingStage(this);
		simulationConditionChoice.setChoosingStage(this);
	}

	private void updateGUI() {
		changeGUIType();
		this.sizeToScene();
	}

	private void setCheckListeners() {
		btApply.setOnAction(e -> {
			result = null;
			boolean validChoice = checkSelection();
			if (!validChoice) {
				showInvalidSelection();
				return;
			}
			result = this.extractItem(simulation);
			this.close();
		});
	}

	private boolean checkSelection() {
		if (simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO && !simulationConditionChoice.checkProperty()) {
			return simulationMonteCarloChoice.checkSelection();
		}
		if (!simulationConditionChoice.checkSelection()) {
			// TODO
			return false;
		}
		SimulationType type = simulationConditionChoice.simulationChoice().getSelectionModel().getSelectedItem();
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


	private SimulationItem extractItem(SimulationModel simulation) {
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		SimulationType type;
		if (simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO && !simulationConditionChoice.checkProperty()) {
			type = SimulationType.MONTE_CARLO_SIMULATION;
		} else {
			type = simulationConditionChoice.simulationChoice().getSelectionModel().getSelectedItem();
		}
		return new SimulationItem(id, simulation.getPath(), type, this.extractInformation());
	}

	private Map<String, Object> extractInformation() {
		Map<String, Object> information = new HashMap<>();

		information.putAll(simulationMonteCarloChoice.extractInformation());
		information.putAll(simulationConditionChoice.extractInformation());

		if (simulationMode.getMode() == SimulationMode.Mode.BLACK_BOX || simulationConditionChoice.checkProperty()) {
			SimulationType simulationType = simulationConditionChoice.simulationChoice().getSelectionModel().getSelectedItem();
			information.putAll(simulationPropertyChoice.extractInformation(simulationType));
			switch (simulationType) {
				case MONTE_CARLO_SIMULATION:
					break;
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
		inputBox.getChildren().removeAll(simulationMonteCarloChoice, simulationConditionChoice, simulationPropertyChoice, simulationHypothesisChoice, simulationEstimationChoice);
		SimulationMode.Mode mode = simulationMode.getMode();

		SimulationType type = simulationConditionChoice.checkProperty() ? simulationConditionChoice.simulationChoice().getSelectionModel().getSelectedItem() : SimulationType.MONTE_CARLO_SIMULATION;
		if (type != null) {
			if (type == SimulationType.ESTIMATION) {
				inputBox.getChildren().add(0, simulationEstimationChoice);
			} else if (type == SimulationType.HYPOTHESIS_TEST) {
				inputBox.getChildren().add(0, simulationHypothesisChoice);
			}
		}
		simulationPropertyChoice.updateCheck(type);

		if (simulationConditionChoice.checkProperty()) {
			inputBox.getChildren().add(0, simulationPropertyChoice);
		}

		inputBox.getChildren().add(0, simulationConditionChoice);

		if (mode == SimulationMode.Mode.MONTE_CARLO) {
			inputBox.getChildren().add(0, simulationMonteCarloChoice);
		}

		// Change order so that validation task id is always at the bottom
	}

	@FXML
	public void cancel() {
		this.close();
	}

	public void setData(SimulationItem item) {
		SimulationType type = item.getType();
		Map<String, Object> information = item.getInformation();
		simulationMonteCarloChoice.setInformation(information);
		simulationConditionChoice.setInformation(type, information);
		simulationPropertyChoice.setInformation(information);
		simulationEstimationChoice.setInformation(information);
		simulationHypothesisChoice.setInformation(information);
	}

	public void reset() {
		simulationMonteCarloChoice.reset();
		simulationConditionChoice.reset();
		simulationPropertyChoice.reset();
		simulationEstimationChoice.reset();
		simulationHypothesisChoice.reset();
	}

	public SimulationItem getResult() {
		return result;
	}

	public void setSimulation(SimulationModel simulation) {
		this.simulation = simulation;
		updateGUI();
	}
}
