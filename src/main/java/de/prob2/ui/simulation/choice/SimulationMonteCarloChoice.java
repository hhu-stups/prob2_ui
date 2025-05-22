package de.prob2.ui.simulation.choice;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.SimulationMode;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

@FXMLInjected
@Singleton
public final class SimulationMonteCarloChoice extends GridPane {
	@FXML
	private Label lbSimulations;

	@FXML
	private TextField tfSimulations;

	private final SimulationMode simulationMode;


	@Inject
	private SimulationMonteCarloChoice(final StageManager stageManager, final SimulationMode simulationMode) {
		super();
		this.simulationMode = simulationMode;
		stageManager.loadFXML(this, "simulation_monte_carlo_choice.fxml");
	}

	@FXML
	private void initialize() {
		lbSimulations.visibleProperty().bind(Bindings.createBooleanBinding(() -> simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO, simulationMode.modeProperty()));
		tfSimulations.visibleProperty().bind(Bindings.createBooleanBinding(() -> simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO, simulationMode.modeProperty()));
	}

	public boolean checkSelection() {
		try {
			Integer.parseInt(tfSimulations.getText());
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public Map<String, Object> extractInformation() {
		Map<String, Object> information = new HashMap<>();
		if(simulationMode.getMode() == SimulationMode.Mode.MONTE_CARLO) {
			information.put("EXECUTIONS", Integer.parseInt(tfSimulations.getText()));
		}
		return information;
	}

	public void setInformation(Map<String, Object> object) {
		if(object.containsKey("EXECUTIONS")) {
			tfSimulations.setText(object.get("EXECUTIONS").toString());
		}
	}

	public void reset() {
		tfSimulations.clear();
	}

}
