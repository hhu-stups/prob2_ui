package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javax.inject.Inject;
import java.util.Map;

@FXMLInjected
public class SimulationHypothesisChoice extends SimulationMonteCarloChoice {

    @FXML
    private Label lbMonteCarloTime;

    @FXML
    private TextField tfMonteCarloTime;

    @FXML
    private ChoiceBox<SimulationPropertyItem> checkingChoice;

    @FXML
    private ChoiceBox<SimulationHypothesisChoiceItem> hypothesisCheckingChoice;

    @FXML
    private TextField tfProbability;

    @Inject
    protected SimulationHypothesisChoice(final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "simulation_hypothesis_choice.fxml");
    }

    @FXML
    protected void initialize() {
        super.initialize();
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

    public Map<String, Object> extractInformation() {
        Map<String, Object> information = super.extractInformation();

        information.put("CHECKING_TYPE", checkingChoice.getSelectionModel().getSelectedItem().getCheckingType());
        information.put("HYPOTHESIS_CHECKING_TYPE", hypothesisCheckingChoice.getSelectionModel().getSelectedItem().getCheckingType());
        information.put("PROBABILITY", Double.parseDouble(tfProbability.getText()));

        SimulationPropertyItem checkingChoiceItem = checkingChoice.getSelectionModel().getSelectedItem();
        if(checkingChoiceItem != null && checkingChoiceItem.getCheckingType() == SimulationCheckingType.TIMING) {
            information.put("TIME", Integer.parseInt(tfMonteCarloTime.getText()));
        }
        return information;
    }

}
