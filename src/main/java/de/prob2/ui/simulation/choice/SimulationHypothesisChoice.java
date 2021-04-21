package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@FXMLInjected
public class SimulationHypothesisChoice extends SimulationAbstractMonteCarloChoice {

	public static class SimulationHypothesisChoiceItem {

		private final SimulationHypothesisChecker.HypothesisCheckingType checkingType;

		public SimulationHypothesisChoiceItem(@NamedArg("checkingType") SimulationHypothesisChecker.HypothesisCheckingType checkingType) {
			this.checkingType = checkingType;
		}

		@Override
		public String toString() {
			return checkingType.getName();
		}

		public SimulationHypothesisChecker.HypothesisCheckingType getCheckingType() {
			return checkingType;
		}

	}

	@FXML
	private ChoiceBox<SimulationHypothesisChoiceItem> hypothesisCheckingChoice;

	@FXML
	private TextField tfProbability;

	@FXML
	private TextField tfSignificance;

	@Inject
	protected SimulationHypothesisChoice(final StageManager stageManager) {
		super(stageManager, "simulation_hypothesis_choice.fxml");
	}

	@Override
	public boolean checkSelection() {
		boolean selection = super.checkSelection();
		if(!selection) {
			return selection;
		}
		try {
			double probability = Double.parseDouble(tfProbability.getText());
			double significance = Double.parseDouble(tfSignificance.getText());
			if(probability >= 1.0 || probability <= 0.0 || significance <= 0.0) {
				return false;
			}
			switch (hypothesisCheckingChoice.getSelectionModel().getSelectedItem().getCheckingType()) {
				case TWO_TAILED:
					return significance*2 <= Math.min(probability, 1 - probability);
				case RIGHT_TAILED:
					return significance <= 1 - probability;
				case LEFT_TAILED:
					return significance <= probability;
				default:
					break;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public Map<String, Object> extractInformation() {
		Map<String, Object> information = super.extractInformation();
		information.put("HYPOTHESIS_CHECKING_TYPE", hypothesisCheckingChoice.getSelectionModel().getSelectedItem().getCheckingType());
		information.put("PROBABILITY", Double.parseDouble(tfProbability.getText()));
		information.put("SIGNIFICANCE", Double.parseDouble(tfSignificance.getText()));
		return information;
	}

}
