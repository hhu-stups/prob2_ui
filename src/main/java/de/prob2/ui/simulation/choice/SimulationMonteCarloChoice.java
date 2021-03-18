package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.check.SimulationMonteCarlo;
import javafx.beans.NamedArg;
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

    public static class SimulationStartingItem {

        private final SimulationMonteCarlo.StartingType startingType;

        public SimulationStartingItem(@NamedArg("startingType") SimulationMonteCarlo.StartingType startingType) {
            this.startingType = startingType;
        }

        @Override
        public String toString() {
            return startingType.getName();
        }

        public SimulationMonteCarlo.StartingType getStartingType() {
            return startingType;
        }

    }

    public static class SimulationEndingItem {

        private final SimulationMonteCarlo.EndingType endingType;

        public SimulationEndingItem(@NamedArg("endingType") SimulationMonteCarlo.EndingType endingType) {
            this.endingType = endingType;
        }

        @Override
        public String toString() {
            return endingType.getName();
        }

        public SimulationMonteCarlo.EndingType getEndingType() {
            return endingType;
        }

    }

    public static class SimulationPropertyItem {

        private final SimulationCheckingType checkingType;

        public SimulationPropertyItem(@NamedArg("checkingType") SimulationCheckingType checkingType) {
            this.checkingType = checkingType;
        }

        @Override
        public String toString() {
            return checkingType.getName();
        }

        public SimulationCheckingType getCheckingType() {
            return checkingType;
        }

    }

    protected SimulationChoosingStage choosingStage;

    @FXML
    protected TextField tfSimulations;

    @FXML
    protected Label lbStartAfter;

    @FXML
    protected TextField tfStartAfter;

    @FXML
    protected Label lbStartingPredicate;

    @FXML
    protected TextField tfStartingPredicate;

    @FXML
    protected Label lbStartingTime;

    @FXML
    protected TextField tfStartingTime;

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
    protected ChoiceBox<SimulationStartingItem> startingChoice;

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
        startingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            this.getChildren().removeAll(lbStartAfter, tfStartAfter, lbStartingPredicate, tfStartingPredicate, lbStartingTime, tfStartingTime);
            if(to != null) {
                switch (to.getStartingType()) {
                    case NO_CONDITION:
                        break;
                    case START_AFTER_STEPS:
                        this.add(lbStartAfter, 1, 3);
                        this.add(tfStartAfter, 2, 3);
                        break;
                    case STARTING_PREDICATE:
                        this.add(lbStartingPredicate, 1, 3);
                        this.add(tfStartingPredicate, 2, 3);
                        break;
                    case STARTING_TIME:
                        this.add(lbStartingTime, 1, 3);
                        this.add(tfStartingTime, 2, 3);
                        break;
                    default:
                        break;
                }
            }
            choosingStage.sizeToScene();
        });

        endingChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
            this.getChildren().removeAll(lbSteps, tfSteps, lbEndingPredicate, tfEndingPredicate, lbEndingTime, tfEndingTime);
            if(to != null) {
                switch (to.getEndingType()) {
                    case NUMBER_STEPS:
                        this.add(lbSteps, 1, 5);
                        this.add(tfSteps, 2, 5);
                        break;
                    case ENDING_PREDICATE:
                        this.add(lbEndingPredicate, 1, 5);
                        this.add(tfEndingPredicate, 2, 5);
                        break;
                    case ENDING_TIME:
                        this.add(lbEndingTime, 1, 5);
                        this.add(tfEndingTime, 2, 5);
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

        SimulationStartingItem startingItem = startingChoice.getSelectionModel().getSelectedItem();
        if(startingItem != null) {
            switch (startingItem.getStartingType()) {
                case NO_CONDITION:
                    break;
                case START_AFTER_STEPS:
                    information.put("START_AFTER_STEPS", Integer.parseInt(tfStartAfter.getText()));
                    break;
                case STARTING_PREDICATE:
                    information.put("STARTING_PREDICATE", tfStartingPredicate.getText());
                    break;
                case STARTING_TIME:
                    information.put("STARTING_TIME", Integer.parseInt(tfStartingTime.getText()));
                    break;
                default:
                    break;
            }
        }

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
