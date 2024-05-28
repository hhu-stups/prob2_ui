package de.prob2.ui.simulation.choice;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;

import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

@FXMLInjected
public class SimulationEstimationChoice extends GridPane {

	public static class SimulationEstimationChoiceItem {

		private final SimulationEstimator.EstimationType estimationType;

		public SimulationEstimationChoiceItem(@NamedArg("estimationType") SimulationEstimator.EstimationType estimationType) {
			this.estimationType = estimationType;
		}

		@Override
		public String toString() {
			return estimationType.name();
		}

		public String getName(I18n i18n) {
			return i18n.translate(estimationType.getKey());
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

	private final Injector injector;

	private final I18n i18n;

	@Inject
	private SimulationEstimationChoice(final Injector injector, final StageManager stageManager, final I18n i18n) {
		super();
		stageManager.loadFXML(this, "simulation_estimation_choice.fxml");
		this.injector = injector;
		this.i18n = i18n;
	}

	@FXML
	private void initialize() {
		estimationChoice.setConverter(new StringConverter<>() {
			@Override
			public String toString(SimulationEstimationChoice.SimulationEstimationChoiceItem object) {
				if (object == null) {
					return "";
				}
				return object.getName(i18n);
			}

			@Override
			public SimulationEstimationChoice.SimulationEstimationChoiceItem fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to SimulationEstimationChoiceItem not supported");
			}
		});
	}

	public boolean checkSelection() {
		SimulationPropertyChoice simulationPropertyChoice = injector.getInstance(SimulationPropertyChoice.class);
		SimulationCheckingType checkingType = simulationPropertyChoice.getCheckingChoice().getSelectionModel().getSelectedItem().getCheckingType();
		boolean estimateProbability = checkingType != SimulationCheckingType.AVERAGE && checkingType != SimulationCheckingType.AVERAGE_MEAN_BETWEEN_STEPS &&
				checkingType != SimulationCheckingType.SUM && checkingType != SimulationCheckingType.SUM_MEAN_BETWEEN_STEPS &&
				checkingType != SimulationCheckingType.MINIMUM && checkingType != SimulationCheckingType.MINIMUM_MEAN_BETWEEN_STEPS &&
				checkingType != SimulationCheckingType.MAXIMUM && checkingType != SimulationCheckingType.MAXIMUM_MEAN_BETWEEN_STEPS;

		try {
			double desiredValue = Double.parseDouble(tfDesiredValue.getText());
			double epsilon = Double.parseDouble(tfEpsilon.getText());
			if(estimateProbability) {
				switch (estimationChoice.getSelectionModel().getSelectedItem().getEstimationType()) {
					case MINIMUM:
					case MAXIMUM:
					case MEAN:
						if (desiredValue <= 0.0 || desiredValue >= 1.0 || epsilon <= 0.0) {
							return false;
						}
						return epsilon <= Math.min(desiredValue, 1 - desiredValue);
					default:
						break;
				}
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

	public void setInformation(Map<String, Object> object) {
		if(object.containsKey("ESTIMATION_TYPE")) {
			estimationChoice.getSelectionModel().select(new SimulationEstimationChoiceItem(SimulationEstimator.EstimationType.valueOf(object.get("ESTIMATION_TYPE").toString())));
		}

		if(object.containsKey("DESIRED_VALUE")) {
			tfDesiredValue.setText(object.get("DESIRED_VALUE").toString());
		}

		if(object.containsKey("EPSILON")) {
			tfEpsilon.setText(object.get("EPSILON").toString());
		}
	}

	public void reset() {
		estimationChoice.getSelectionModel().clearSelection();
		tfDesiredValue.clear();
		tfEpsilon.clear();
	}

}
