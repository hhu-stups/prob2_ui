package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@FXMLInjected
public class SimulationEstimationChoice extends GridPane {

	public static class SimulationEstimationChoiceItem {

		private final SimulationEstimator.EstimationType estimationType;

		public SimulationEstimationChoiceItem(@NamedArg("estimationType") SimulationEstimator.EstimationType estimationType) {
			this.estimationType = estimationType;
		}

		@Override
		public String toString() {
			return estimationType.getName();
		}

		public SimulationEstimator.EstimationType getEstimationType() {
			return estimationType;
		}

	}

	@FXML
	private ChoiceBox<SimulationEstimationChoiceItem> estimationChoice;

	@FXML
	private TextField tfDesiredValue;

	@FXML
	private TextField tfEpsilon;

	@Inject
	protected SimulationEstimationChoice(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "simulation_estimation_choice.fxml");
	}

	public boolean checkSelection() {
		try {
			double desiredValue = Double.parseDouble(tfDesiredValue.getText());
			double epsilon = Double.parseDouble(tfEpsilon.getText());
			switch (estimationChoice.getSelectionModel().getSelectedItem().getEstimationType()) {
				case MINIMUM:
				case MAXIMUM:
				case MEAN:
					if(desiredValue <= 0.0 || desiredValue >= 1.0 || epsilon <= 0.0) {
						return false;
					}
					return epsilon <= Math.min(desiredValue, 1 - desiredValue);
				default:
					break;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public Map<String, Object> extractInformation() {
		Map<String, Object> information = new HashMap<>();
		information.put("ESTIMATION_TYPE", estimationChoice.getSelectionModel().getSelectedItem().getEstimationType());
		information.put("DESIRED_VALUE", Double.parseDouble(tfDesiredValue.getText()));
		information.put("EPSILON", Double.parseDouble(tfEpsilon.getText()));
		return information;
	}

}
