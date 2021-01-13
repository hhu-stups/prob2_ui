package de.prob2.ui.simulation.choice;

import com.google.inject.Injector;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationItemHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@FXMLInjected
public class SimulationHypothesisChoice extends GridPane {

    private SimulationChoosingStage choosingStage;

    @FXML
    private Label lbMonteCarloTime;

    @FXML
    private TextField tfMonteCarloTime;

    @FXML
    private TextField tfSimulations;

    @FXML
    private Label lbSteps;

    @FXML
    private TextField tfSteps;

    @FXML
    private Label lbEndingPredicate;

    @FXML
    private TextField tfEndingPredicate;

    @FXML
    private Label lbEndingTime;

    @FXML
    private TextField tfEndingTime;

    @FXML
    private ChoiceBox<SimulationPropertyItem> checkingChoice;

    @FXML
    private ChoiceBox<SimulationEndingItem> endingChoice;

    @FXML
    private ChoiceBox<SimulationHypothesisChoiceItem> hypothesisCheckingChoice;

    @FXML
    private TextField tfProbability;

    @Inject
    private SimulationHypothesisChoice(final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "simulation_hypothesis_choice.fxml");
    }

    @FXML
    private void initialize() {
        endingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            this.getChildren().removeAll(lbSteps, tfSteps, lbEndingPredicate, tfEndingPredicate, lbEndingTime, tfEndingTime);
            if(to != null) {
                switch (to.getEndingType()) {
                    case NUMBER_STEPS:
                        this.add(lbSteps, 1, 3);
                        this.add(tfSteps, 2, 3);
                        break;
                    case ENDING_PREDICATE:
                        this.add(lbEndingPredicate, 1, 3);
                        this.add(tfEndingPredicate, 2, 3);
                        break;
                    case ENDING_TIME:
                        this.add(lbEndingTime, 1, 3);
                        this.add(tfEndingTime, 2, 3);
                        break;
                    default:
                        break;
                }
            }
            choosingStage.sizeToScene();
        });

        checkingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            if(to != null && to.getCheckingType() == SimulationCheckingType.TIMING) {
                this.add(lbMonteCarloTime, 1, 8);
                this.add(tfMonteCarloTime, 2, 8);
            } else {
                this.getChildren().remove(lbMonteCarloTime);
                this.getChildren().remove(tfMonteCarloTime);
            }
            choosingStage.sizeToScene();
        });
    }

    public boolean checkSelection() {
        // TODO: Check integer
        return !(tfSteps.getText().isEmpty() && tfSimulations.getText().isEmpty());
    }

    public Map<String, Object> extractInformation() {
        Map<String, Object> information = new HashMap<>();
        information.put("EXECUTIONS", Integer.parseInt(tfSimulations.getText()));

        SimulationEndingItem endingItem = endingChoice.getSelectionModel().getSelectedItem();
        if(endingItem != null) {
            switch(endingItem.getEndingType()) {
                case NUMBER_STEPS:
                    information.put("STEPS_PER_EXECUTION", Integer.parseInt(tfSteps.getText()));
                    break;
                case ENDING_PREDICATE:
                    information.put("ENDING_PREDICATE", tfEndingPredicate.getText());
                    break;
                case ENDING_TIME:
                    information.put("ENDING_TIME", Integer.parseInt(tfEndingTime.getText()));
                    break;
                default:
                    break;
            }
        }

        information.put("CHECKING_TYPE", checkingChoice.getSelectionModel().getSelectedItem().getCheckingType());
        information.put("HYPOTHESIS_CHECKING_TYPE", hypothesisCheckingChoice.getSelectionModel().getSelectedItem().getCheckingType());
        information.put("PROBABILITY", Double.parseDouble(tfProbability.getText()));

        SimulationPropertyItem checkingChoiceItem = checkingChoice.getSelectionModel().getSelectedItem();
        if(checkingChoiceItem != null && checkingChoiceItem.getCheckingType() == SimulationCheckingType.TIMING) {
            information.put("TIME", Integer.parseInt(tfMonteCarloTime.getText()));
        }
        return information;
    }

    public void setSimulationChoosingStage(SimulationChoosingStage choosingStage) {
        this.choosingStage = choosingStage;
    }

    public void clear() {
        tfSimulations.clear();
        tfSteps.clear();
    }

}
