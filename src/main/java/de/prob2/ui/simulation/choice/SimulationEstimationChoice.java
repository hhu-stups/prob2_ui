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
public class SimulationEstimationChoice extends SimulationMonteCarloChoice {

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

    private final List<SimulationCheckingType> PREDICATE_TYPES = Arrays.asList(SimulationCheckingType.PREDICATE_INVARIANT, SimulationCheckingType.PREDICATE_FINAL, SimulationCheckingType.PREDICATE_EVENTUALLY);

    @FXML
    private Label lbMonteCarloTime;

    @FXML
    private TextField tfMonteCarloTime;

    @FXML
    private Label lbPredicate;

    @FXML
    private TextField tfPredicate;

    @FXML
    private ChoiceBox<SimulationPropertyItem> checkingChoice;

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

    @FXML
    protected void initialize() {
        super.initialize();
        checkingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            this.getChildren().remove(lbMonteCarloTime);
            this.getChildren().remove(tfMonteCarloTime);
            this.getChildren().remove(lbPredicate);
            this.getChildren().remove(tfPredicate);
            if(to != null) {
                if(to.getCheckingType() == SimulationCheckingType.TIMING) {
                    this.add(lbMonteCarloTime, 1, 11);
                    this.add(tfMonteCarloTime, 2, 11);
                }
                if(PREDICATE_TYPES.contains(to.getCheckingType())) {
                    this.add(lbPredicate, 1, 7);
                    this.add(tfPredicate, 2, 7);
                }
            }
            choosingStage.sizeToScene();
        });
    }

    public Map<String, Object> extractInformation() {
        Map<String, Object> information = super.extractInformation();
        SimulationPropertyItem checkingChoiceItem = checkingChoice.getSelectionModel().getSelectedItem();

        information.put("CHECKING_TYPE", checkingChoiceItem.getCheckingType());
        information.put("ESTIMATION_TYPE", estimationChoice.getSelectionModel().getSelectedItem().getEstimationType());
        information.put("DESIRED_VALUE", Double.parseDouble(tfDesiredValue.getText()));
        information.put("EPSILON", Double.parseDouble(tfEpsilon.getText()));

        if(PREDICATE_TYPES.contains(checkingChoiceItem.getCheckingType())) {
            information.put("PREDICATE", tfPredicate.getText());
        }

        if(checkingChoiceItem.getCheckingType() == SimulationCheckingType.TIMING) {
            information.put("TIME", Integer.parseInt(tfMonteCarloTime.getText()));
        }
        return information;
    }

}
