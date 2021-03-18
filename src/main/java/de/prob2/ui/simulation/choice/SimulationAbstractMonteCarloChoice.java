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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@FXMLInjected
public class SimulationAbstractMonteCarloChoice extends SimulationMonteCarloChoice {

    protected final List<SimulationCheckingType> PREDICATE_TYPES = Arrays.asList(SimulationCheckingType.PREDICATE_INVARIANT, SimulationCheckingType.PREDICATE_FINAL, SimulationCheckingType.PREDICATE_EVENTUALLY);

    @FXML
    protected Label lbMonteCarloTime;

    @FXML
    protected TextField tfMonteCarloTime;

    @FXML
    protected Label lbPredicate;

    @FXML
    protected TextField tfPredicate;

    @FXML
    protected ChoiceBox<SimulationPropertyItem> checkingChoice;

    protected SimulationAbstractMonteCarloChoice(final StageManager stageManager, final String fxmlResource) {
        super();
        stageManager.loadFXML(this, fxmlResource);
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

}
