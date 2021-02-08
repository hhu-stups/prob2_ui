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
public class SimulationEstimationChoice extends SimulationMonteCarloChoice {

    @FXML
    private Label lbMonteCarloTime;

    @FXML
    private TextField tfMonteCarloTime;

    @FXML
    private ChoiceBox<SimulationPropertyItem> checkingChoice;

    @FXML
    private ChoiceBox<SimulationEstimationChoiceItem> estimationChoice;

    @FXML
    private TextField tfDesiredValue;

    @FXML
    private TextField tfFaultTolerance;

    @Inject
    protected SimulationEstimationChoice(final StageManager stageManager) {
        super();
        stageManager.loadFXML(this, "simulation_estimation_choice.fxml");
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
        SimulationPropertyItem checkingChoiceItem = checkingChoice.getSelectionModel().getSelectedItem();

        information.put("CHECKING_TYPE", checkingChoiceItem.getCheckingType());
        information.put("ESTIMATION_TYPE", estimationChoice.getSelectionModel().getSelectedItem().getEstimationType());
        information.put("DESIRED_VALUE", Double.parseDouble(tfDesiredValue.getText()));
        information.put("FAULT_TOLERANCE", Double.parseDouble(tfFaultTolerance.getText()));

        if(checkingChoiceItem.getCheckingType() == SimulationCheckingType.TIMING) {
            information.put("TIME", Integer.parseInt(tfMonteCarloTime.getText()));
        }
        return information;
    }

}
