package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@FXMLInjected
public class SimulationMonteCarloChoice extends GridPane {

    protected SimulationChoosingStage choosingStage;

    @FXML
    protected TextField tfSimulations;

    @FXML
    protected Label lbSteps;

    @FXML
    protected TextField tfSteps;

    @FXML
    protected Label lbEndingPredicate;

    @FXML
    protected TextField tfEndingPredicate;

    @FXML
    protected Label lbEndingTime;

    @FXML
    protected TextField tfEndingTime;

    @FXML
    protected ChoiceBox<SimulationEndingItem> endingChoice;

    @Inject
    protected SimulationMonteCarloChoice(final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "simulation_monte_carlo_choice.fxml");
    }

    protected SimulationMonteCarloChoice() {
        //Default constructor for super classes using other FXML file
    }

    @FXML
    protected void initialize() {
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
