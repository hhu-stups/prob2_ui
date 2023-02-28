package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@FXMLInjected
public class SimulationHypothesisChoice extends GridPane {

	public static class SimulationHypothesisChoiceItem {

		private final SimulationHypothesisChecker.HypothesisCheckingType checkingType;

		public SimulationHypothesisChoiceItem(@NamedArg("checkingType") SimulationHypothesisChecker.HypothesisCheckingType checkingType) {
			this.checkingType = checkingType;
		}

		@Override
		public String toString() {
			return checkingType.name();
		}

		public String getName(I18n i18n) {
			return i18n.translate(checkingType.getKey());
		}

		public SimulationHypothesisChecker.HypothesisCheckingType getCheckingType() {
			return checkingType;
		}

	}

	private final I18n i18n;

	@FXML
	private ChoiceBox<SimulationHypothesisChoiceItem> hypothesisCheckingChoice;

	@FXML
	private TextField tfProbability;

	@FXML
	private TextField tfSignificance;

	@Inject
	private SimulationHypothesisChoice(final StageManager stageManager, final I18n i18n) {
		super();
		stageManager.loadFXML(this, "simulation_hypothesis_choice.fxml");
		this.i18n = i18n;
	}

	@FXML
	private void initialize() {
		hypothesisCheckingChoice.setConverter(new StringConverter<SimulationHypothesisChoiceItem>() {
			@Override
			public String toString(SimulationHypothesisChoiceItem object) {
				if(object == null) {
					return "";
				}
				return object.getName(i18n);
			}

			@Override
			public SimulationHypothesisChoiceItem fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to SimulationHypothesisChoiceItem not supported");
			}
		});
	}

	public boolean checkSelection() {
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
		Map<String, Object> information = new HashMap<>();
		information.put("HYPOTHESIS_CHECKING_TYPE", hypothesisCheckingChoice.getSelectionModel().getSelectedItem().getCheckingType());
		information.put("PROBABILITY", Double.parseDouble(tfProbability.getText()));
		information.put("SIGNIFICANCE", Double.parseDouble(tfSignificance.getText()));
		return information;
	}

}
