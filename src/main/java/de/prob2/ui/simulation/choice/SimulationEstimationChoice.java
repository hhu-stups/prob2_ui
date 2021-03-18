package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@FXMLInjected
public class SimulationEstimationChoice extends SimulationAbstractMonteCarloChoice {

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
        super(stageManager, "simulation_estimation_choice.fxml");
    }

    public Map<String, Object> extractInformation() {
        Map<String, Object> information = super.extractInformation();
        information.put("ESTIMATION_TYPE", estimationChoice.getSelectionModel().getSelectedItem().getEstimationType());
        information.put("DESIRED_VALUE", Double.parseDouble(tfDesiredValue.getText()));
        information.put("EPSILON", Double.parseDouble(tfEpsilon.getText()));
        return information;
    }

}
